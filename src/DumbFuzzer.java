import java.io.FileWriter;
import java.util.Random;
import java.io.IOException;  // Import the IOException class to handle errors


public class DumbFuzzer {
    
    public static double chanceOfOddGlobal = 0.5; // chance that the generated number is negative 

    public static int randomNumberGenerator(int upperBound, boolean generateNegative) {
        Random rand = new Random();
        // Generate a non-zero integer
        // Reduce number range, add 1 at the end
        // If upperBound is less than 1, set randInt to 1
        int randInt;
        if (upperBound <= 1) {
            randInt = 1;
        } else {
            randInt = rand.nextInt(upperBound - 1) + 1;
        }

        // Flip the number to negative with certain probability if required
        if (generateNegative) {
            double flipToNegative = Math.random();
            if (flipToNegative < chanceOfOddGlobal) {
                randInt = -randInt;
            }
        }
        return randInt;
    }

    public static String numberToString(int number) {
        return String.valueOf(number);
    }

    public static void main(String[] args) {
        // Generate an CNF
        System.out.println("Main dumb fuzzer ran");
    }
        
    

    // Generates a random but valid CNF file
    public static void generateCNF(String filePath, int numVariablesGlobal, int numLinesGlobal, int maxVariablesPerLineGlobal){
        StringBuilder outString = new StringBuilder();
        // Generate first line
        // Format: p cnf numVariables, numLines
        outString.append("p cnf ");
        int numVariables = randomNumberGenerator(numVariablesGlobal, false);
        int numLines = randomNumberGenerator(numLinesGlobal, false);
        outString.append(numberToString(numVariables)).append(" ");
        outString.append(numberToString(numLines)).append("\n");

        // For each following lines generate a valid random integer
        for (int currentLineNum = 0; currentLineNum < numLines; currentLineNum++) {
            // Generate the number of elements on each line
            int numAtoms = randomNumberGenerator(maxVariablesPerLineGlobal, false);
            for (int currentAtom = 0; currentAtom < numAtoms; currentAtom++) {
                int currentVar = randomNumberGenerator(numVariables, true);
                outString.append(currentVar);
                outString.append(" ");
            }
            // End the line
            outString.append("0\n");
        }

        
        // Saves the generated string to a file 
        try {
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(outString.toString());
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Generates a random CNF file but the first line specifies a far smaller number of variables and lines
    // than there are in the file
    public static void overflow(String filePath, int numVariablesGlobal, int numLinesGlobal, int maxVariablesPerLineGlobal){
        StringBuilder outString = new StringBuilder();
        // Generate first line
        // Format: p cnf numVariables, numLines
        outString.append("p cnf ");
        int numVariables = randomNumberGenerator(numVariablesGlobal, false);
        int numLines = randomNumberGenerator(numLinesGlobal, false);
        outString.append(numberToString(10)).append(" ");
        outString.append(numberToString(10)).append("\n");

        // For each following lines generate a valid random integer
        for (int currentLineNum = 0; currentLineNum < numLines; currentLineNum++) {
            // Generate the line size
            int numAtoms = randomNumberGenerator(maxVariablesPerLineGlobal, false);
            for (int currentAtom = 0; currentAtom < numAtoms; currentAtom++) {
                int currentVar = randomNumberGenerator(numVariables, true);
                outString.append(currentVar);
                outString.append(" ");
            }
            outString.append("0\n");
        }

        // Saves the generated string to a file 
        try {
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(outString.toString());
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Creates lines that either contain 1 or Zero elements for some lines and normal lines elsewhere
    public static void wrongLineSizeCNF(String filePath, int numVariablesGlobal, int numLinesGlobal, double chanceOfOneElemLine, double chanceOfZeroElemLine  ){
        
        StringBuilder outString = new StringBuilder();
        // Generate first line
        // Format: p cnf numVariables, numLines
        outString.append("p cnf ");
        int numVariables = randomNumberGenerator(numVariablesGlobal, false);
        int numLines = randomNumberGenerator(numLinesGlobal, false);
        outString.append(numberToString(numVariables)).append(" ");
        outString.append(numberToString(numLines)).append("\n");

        
        // For each line either generate a valid line, 1 element or no elements
        for (int currentLineNum = 0; currentLineNum < numLines; currentLineNum++){
            double probOneElem = Math.random();
            double probZeroElem = Math.random();

            // Creates a one element line if the probability executes
            if (probOneElem < chanceOfOneElemLine){
                int randomVal = randomNumberGenerator(numVariables, true);
                outString.append(randomVal);
                outString.append(" ");
                outString.append("0\n");
            }

            // Creates an empty line
            else if (probZeroElem < chanceOfZeroElemLine){
                outString.append("0\n");
            }

            // Adds a regular line
            else {
                int currentVar = randomNumberGenerator(numVariables, true);
                outString.append(currentVar);
                outString.append(" ");
                outString.append("0\n");
            }
        }

        // Saves output to CNF file
        try {
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(outString.toString());
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // Drops some zeros from the end of a line
    public static void droppedZerosCNF(String filePath, int numVariablesGlobal, int numLinesGlobal, int maxVariablesPerLineGlobal, double chanceOfDroppedZero){
         StringBuilder outString = new StringBuilder();
        // Generate first line
        // Format: p cnf numVariables, numLines
        outString.append("p cnf ");
        int numVariables = randomNumberGenerator(numVariablesGlobal, false);
        int numLines = randomNumberGenerator(numLinesGlobal, false);
        outString.append(numberToString(numVariables)).append(" ");
        outString.append(numberToString(numLines)).append("\n");

        // For each following lines
        for (int currentLineNum = 0; currentLineNum < numLines; currentLineNum++) {
            int numAtoms = randomNumberGenerator(maxVariablesPerLineGlobal, false);
            for (int currentAtom = 0; currentAtom < numAtoms; currentAtom++) {
                int currentVar = randomNumberGenerator(numVariables, true);
                outString.append(currentVar);
                outString.append(" ");
            }

            // Drops a zero with 50% probability
            double dropZero = Math.random();
            if (!(dropZero < chanceOfDroppedZero)) {
              outString.append("0\n");
            }
            
        }

        // Saves output string to file
        try {
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(outString.toString());
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        
    }
}
