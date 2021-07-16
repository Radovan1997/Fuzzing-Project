import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

public class FuzzerRuntime {
    static String currentDir = System.getProperty("user.dir");
    static String solverDir;
    static String wellFormedCNFDir;
    static boolean mutationFuzzerEnabled = false;
    static String fuzzedTestsLocation = currentDir + "/fuzzed-tests";
    static int generatedCounter = 0;
    static int savedTestCounter = 0;

    ArrayList<FuzzRun> coveragesInfo = new ArrayList<>();
    CFiles cfiles;

    public FuzzerRuntime(CFiles cfiles) {
        this.cfiles = cfiles; // Store the file names and line counts of each C file in the folder
    }

    public void storeFuzzRun() {
        // Store this run without checking the coverage
        FuzzRun thisFuzzRun = new FuzzRun(cfiles);
        System.out.println("Saving this run without checking coverage");
        coveragesInfo.add(thisFuzzRun); // Add the coverage profile for future checks
    }

    public int storeAndCheckFuzzRun() {
        // For each file, find the corresponding Gcov file, parse and store
        FuzzRun thisFuzzRun = new FuzzRun(cfiles);
        // Check this FuzzRun against all previous runs
        try {
            boolean hasUniqueCoverage = checkFuzzRunIsUnique(thisFuzzRun);
            int returnCode = -1;
            if (hasUniqueCoverage) {
                // Pick the coverage that has the least coverage percentage
                System.out.println("This is a new coverage profile, saving");
                int leastCoverageIdx = 0;
                double lowestRunningCoverage = Double.MAX_VALUE;
                for (int i = 0; i < coveragesInfo.size(); i++) {
                    // Go through each FuzzRun and find the one with smallest coverage
                    FuzzRun prevRun = coveragesInfo.get(i);
                    double prevRunPercentage = prevRun.coveragePercentage;
                    if (lowestRunningCoverage > prevRunPercentage) {
                        leastCoverageIdx = i;
                        lowestRunningCoverage = prevRunPercentage;
                    }
                }
                // Overwrite the coverage data and return index (for the test file to be overwritten)
                coveragesInfo.set(leastCoverageIdx, thisFuzzRun);
                returnCode = leastCoverageIdx;
            }
            return returnCode;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private boolean checkFuzzRunIsUnique(FuzzRun thisFuzzRun) throws IOException {
        if (coveragesInfo.size() == 0) {
            // This is the first coverage file, return true
            return true;
        }
        for (FuzzRun prevFuzzRun : coveragesInfo) {
            // Check previous FuzzRuns for uniqueness
            boolean thisRunDifferent = false;
            for (String file : prevFuzzRun.runCounts.keySet()) {
                // For each file in this solver...
                ArrayList<Integer> prevRunCoverage = prevFuzzRun.runCounts.get(file);
                ArrayList<Integer> thisRunCoverage = thisFuzzRun.runCounts.get(file);
                // ... compare each file in the run
                if (prevRunCoverage.size() != thisRunCoverage.size()) {
                    throw new IOException("Line count not the same");
                }
                for (int lineNum = 0; lineNum < prevRunCoverage.size(); lineNum++) {
                    if (!prevRunCoverage.get(lineNum).equals(thisRunCoverage.get(lineNum))) {
                        // This file's coverage is different, mark it
                        thisRunDifferent = true;
                        break;
                    }
                }
                if (thisRunDifferent) {
                    // Stop the search if there is a difference
                    break;
                }
            }
            if (!thisRunDifferent) {
                // This profile is identical, stop searching and return false
                return false;
            }
        }
        // All profiles have been checked and this profile is unique, return true
        return true;
    }

    public static void main(String[] args) throws IOException {
        // Decide if the paths given is absolute or relative
        // Store the solver's directory
        if (args[0].charAt(0) == '/') {
            solverDir = args[0];
        } else {
            solverDir = currentDir + "/" + args[0];
        }
        System.out.println("solverDir: " + solverDir);

        // Get the inputs folder for mutation fuzzer, if the given address is valid
        File[] listOfAllWellFormedCNFFiles;
        if (args.length > 1) {
            if (args[1].charAt(0) == '/') {
                wellFormedCNFDir = args[1];
            } else {
                wellFormedCNFDir = currentDir + "/" + args[1];
            }
            mutationFuzzerEnabled = true;
            listOfAllWellFormedCNFFiles = new File(wellFormedCNFDir).listFiles();
            if (listOfAllWellFormedCNFFiles == null || listOfAllWellFormedCNFFiles.length == 0) {
                mutationFuzzerEnabled = false;
                System.out.println("Mutation fuzzer *not* enabled");
            }
        } else {
            listOfAllWellFormedCNFFiles = new File[0];
        }

        // Create CFile object for each file
        File filesInSolverDir = new File(solverDir);

        File[] listOfAllC = filesInSolverDir.listFiles((file, s) -> s.endsWith(".c"));
        assert listOfAllC != null;
        System.out.println("Number of all files: "+ filesInSolverDir.listFiles().length);
        System.out.println("Number of C files: " + listOfAllC.length);
        CFiles cfiles = new CFiles(listOfAllC);

        // If there is no C file in the folder, return
        if (listOfAllC.length == 0) {
            return;
        }

        FuzzerRuntime fuzzerRuntime = new FuzzerRuntime(cfiles);

        // Parameters for generative fuzzer
        int numLinesGlobal = 50;
        int numVariablesGlobal = 50;
        int maxVariablesPerLineGlobal = 10;
        

        //Wrong line probabilities
        double chanceOfOneElemLine = 0.15;
        double chanceOfZeroElemLine = 0.1;

        //Dropped zero probability
        double chanceOfDroppedZero = 0.25;
        double chanceOfMutation = 0.1;

        // Run edge CNF cases first, if files exist
        String badlyFormattedCNFPath = currentDir + "/badly-formatted-cnfs";
        File[] listOfPreWrittenBadCNFs = new File(badlyFormattedCNFPath).listFiles();
        try {
            assert listOfPreWrittenBadCNFs != null;
            for (File cnfFile : listOfPreWrittenBadCNFs) {
                // Execute fuzzer
                executeFuzzer(fuzzerRuntime, cnfFile.getAbsolutePath(), -1);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        
        
        while (true) {
            // Generate new random fuzzer file
            // Increase size of file progressively
            numLinesGlobal += 40;
            numVariablesGlobal += 30;
            maxVariablesPerLineGlobal += 20;
            
            chanceOfOneElemLine += 0.02;
            if (chanceOfOneElemLine > 1.0){
                chanceOfOneElemLine = 0.15;
            }
            chanceOfZeroElemLine += 0.015;
            if (chanceOfZeroElemLine > 1.0){
                chanceOfZeroElemLine = 0.1;
            }
            
            chanceOfDroppedZero += 0.08;
            if (chanceOfDroppedZero > 1.0){
                chanceOfDroppedZero = 0.25;
            }

            chanceOfMutation += 0.07;
            if (chanceOfMutation > 1.0){
                chanceOfMutation = 0.1;
            }
            
            String randomScriptLoc = currentDir + "/random_cnf.cnf";

            // Use different fuzzers
            // First check if well-formed CNFs are present
            int[] fuzzerWeights;
            File randomCNFFile = null;

            // Pick the fuzzer to use on a weighted basis
            assert listOfAllWellFormedCNFFiles != null;
            if (!mutationFuzzerEnabled) {
                fuzzerWeights = new int[]{4, 1, 1, 0, 0, 0, 1, 2};
            } else {
                randomCNFFile = listOfAllWellFormedCNFFiles[
                        new Random().nextInt(listOfAllWellFormedCNFFiles.length)];
                fuzzerWeights = new int[]{4, 1, 1, 2, 1, 1, 1, 2};
            }
            int weightsTotal = IntStream.of(fuzzerWeights).sum();

            double randomNumber = Math.random() * weightsTotal;
            int j = 0;
            int fuzzerIndex = 0;
            for (int i = 0; i < fuzzerWeights.length; i++) {
                j = j + fuzzerWeights[i];
                if (randomNumber < j) {
                    fuzzerIndex = i;
                    break;
                }
            }

            // Big switch box to pick a fuzzer
            // They all generate a script to ./random-cnf.cnf
            switch (fuzzerIndex) {
                case 0:
                    DumbFuzzer.generateCNF(randomScriptLoc, numVariablesGlobal, numLinesGlobal, maxVariablesPerLineGlobal);
                    break;
                case 1:
                    DumbFuzzer.wrongLineSizeCNF(randomScriptLoc, numVariablesGlobal, numLinesGlobal, chanceOfOneElemLine, chanceOfZeroElemLine);
                    break;
                case 2:
                    DumbFuzzer.droppedZerosCNF(randomScriptLoc, numVariablesGlobal, numLinesGlobal, maxVariablesPerLineGlobal, chanceOfDroppedZero);
                    break;
                case 3:
                    Mutator.randomMutator(randomCNFFile, randomScriptLoc);
                    break;
                case 4:
                    Mutator.randomDrop(randomCNFFile, randomScriptLoc, chanceOfMutation);
                    break;
                case 5:
                    Mutator.randomInts(randomCNFFile, randomScriptLoc);
                    break;
                case 6:
                    DumbFuzzer.overflow(randomScriptLoc, numVariablesGlobal, numLinesGlobal, maxVariablesPerLineGlobal);
                    break;
                case 7:
                    Generator.dumbFuzzer(randomScriptLoc);
                    break;
                default:
                    DumbFuzzer.generateCNF(randomScriptLoc, numVariablesGlobal, numLinesGlobal, maxVariablesPerLineGlobal);
                    break;
            }

            DumbFuzzer.generateCNF(randomScriptLoc, numVariablesGlobal, numLinesGlobal, maxVariablesPerLineGlobal);
            generatedCounter++;

            // Execute fuzzer
            executeFuzzer(fuzzerRuntime, randomScriptLoc, fuzzerIndex);
        }
    }

    private static void executeFuzzer(FuzzerRuntime fuzzerRuntime, String scriptLoc, int fuzzerIndex) throws IOException {
        ProcessBuilder pb;
        Process process;
        // Wrap the runsat.sh around timeout - using Unix system's timeout management
        pb = new ProcessBuilder(
                "timeout", "-s", "SIGTERM", "-k",
                "15s", "10s", solverDir + "/runsat.sh", scriptLoc
        );
        pb.directory(new File(solverDir));
        try {
            process = pb.start();

            int exitCode = process.waitFor();

            // Read the error stream to catch sanitizer messages
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String errOutLine = "";
            StringBuilder errOutSB = new StringBuilder();
            while ((errOutLine = br.readLine()) != null) {
                errOutSB.append(errOutLine);
            }

            String errOutString = errOutSB.toString();

            // Check if any sanitiser flagged anything suspicious
            boolean sanitiserFlagged = false;
            String[] sanitisers = {"UndefinedBehaviorSanitizer", "UBSan", "AddressSanitizer"};
            for (String san : sanitisers) {
                if (errOutString.contains(san)) {
                    sanitiserFlagged = true;
                    break;
                }
            }

            // UB detected, attempt to save the file
            if (exitCode != 0 || sanitiserFlagged) {
                // System.out.println("Fuzz test " + generatedCounter);
                System.out.println(">> Error code " + exitCode);
                System.out.println(">> Fuzzer idx " + fuzzerIndex);
                System.out.println(">> Sanitiser flag: " + (sanitiserFlagged ? "true" : "false"));

                // Do coverage check stuff
                String[] gcovCommand;
                gcovCommand = new String[]{
                        "bash", "-c", "gcov " + solverDir + "/*.c"
                };
                pb = new ProcessBuilder(gcovCommand);
                pb.directory(new File(solverDir));
                pb.inheritIO().start().waitFor();

                // Store this fuzz run
                // If the test is the first 20, save the file regardless of coverage
                if (savedTestCounter < 20) {
                    fuzzerRuntime.storeFuzzRun(); // Save the coverage information to the runtime
                    String[] cpCommand = {
                            "cp", scriptLoc,
                            fuzzedTestsLocation + "/" + savedTestCounter % 20 + ".cnf"
                    };
                    pb = new ProcessBuilder(cpCommand);
                    pb.start().waitFor();
                    savedTestCounter++;
                } else {
                    saveFuzzOnLeastCoverage(fuzzerRuntime, scriptLoc);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void saveFuzzOnLeastCoverage(FuzzerRuntime fuzzerRuntime, String scriptLoc)
            throws InterruptedException, IOException {
        ProcessBuilder pb;
        int uniqueEvictionIdx = fuzzerRuntime.storeAndCheckFuzzRun(); // Save and check coverage information to the runtime for uniqueness
        if (uniqueEvictionIdx >= 0) {
            // If there is a file to be evicted, overwrite that file
            String[] cpCommand = {
                    "cp", scriptLoc,
                    fuzzedTestsLocation + "/" + uniqueEvictionIdx + ".cnf"
            };
            pb = new ProcessBuilder(cpCommand);
            pb.start().waitFor();
            savedTestCounter++;
        }
    }

    private static class CFiles {
        // Wrapper for all C file properties for the solver
        HashMap<String, CFile> cFiles = new HashMap<>();

        CFiles(File[] files) {
            for (File f : files) {
                CFile cfile = new CFile(f);
                cFiles.put(cfile.filename, cfile);
            }
        }
    }

    private static class CFile {
        // Wrapper for C File properties
        private final String filename;
        private int lineCount = 0;

        CFile(File file) {
            String thisName = file.getName();
            this.filename = thisName.substring(0, thisName.length() - 2);
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while (reader.readLine() != null) this.lineCount++;
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static class FuzzRun {
        // Keeps coverage information for each fuzzed execution
        HashMap<String, ArrayList<Integer>> runCounts = new HashMap<>(); // File name -> ArrayList of 0/1 indicating execution for each line
        double coveragePercentage;

        FuzzRun(CFiles cfiles) {
            int totalNumLines = 0;
            int totalRunLines = 0;
            for (String filename : cfiles.cFiles.keySet()) {
                ArrayList<Integer> gcovRunCoverage =
                        GcovParser.readAndParseGcov(
                                solverDir + "/" + filename + ".c.gcov",
                                cfiles.cFiles.get(filename).lineCount);
                for (int run : gcovRunCoverage) {
                    totalNumLines++;
                    totalRunLines = totalRunLines + run;
                }
                runCounts.put(filename, gcovRunCoverage);
                coveragePercentage = ((double) totalRunLines) / totalNumLines;
            }
        }

    }
}

class GcovParser {
    public static ArrayList<Integer> readAndParseGcov(String gcovFilePath, int lineCount) {
        ArrayList<Integer> executionCount = new ArrayList<>();
        // Init list
        for (int i = 0; i < lineCount; i++) {
            executionCount.add(0);
        }
        try {
            File gcovFile = new File(gcovFilePath);
            Scanner fileScanner = new Scanner(gcovFile);
            while (fileScanner.hasNextLine()) {
                String nextLine = fileScanner.nextLine();
                // Split line by colon
                String[] splitLine = nextLine.split(":");
                if (splitLine.length < 3) {
                    // Skip line if there are less than three elements after splitting by colon
                    // Meaning this line in gcov file does not reflect a line of code
                    continue;
                }
                String lineExecutionStats = splitLine[0].trim();
                String lineNumberString = splitLine[1].trim();
                int lineNumber = Integer.parseInt(lineNumberString); // Throws exception if this is not a number

                // If the line contains star, remove it
                if (lineExecutionStats.endsWith("*")) {
                    lineExecutionStats = lineExecutionStats.substring(0, lineExecutionStats.length() - 1);
                }

                // Try parsing the line
                // If the line execution count is not a number, it is not a line of executable code
                int lineExecutionCount;
                try {
                    Integer.parseInt(lineExecutionStats);
                    lineExecutionCount = 1;
                } catch (NumberFormatException e) {
                    lineExecutionCount = 0;
                }

                int arrayId = lineNumber - 1;
                if (arrayId < 0) {
                    continue; // Skip first line
                }

                // If an entry already exists in the map and it is empty, replace empty with a actual value
                if (executionCount.get(arrayId) <= 0 && lineExecutionCount > 0) {
                    executionCount.set(arrayId, lineExecutionCount);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return executionCount;
    }
}
