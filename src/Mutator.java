import java.io.FileWriter;
import java.io.FileReader;
import java.util.Random;
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.io.File;
import java.lang.*;

//** Takes in files as input, and mutates these based on given rules and probabilities
 //
public class Mutator{

    //The variable which declares the probability of a variable being negated    
    public static double chanceOfOddGlobal = 0.5;

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


    //Read in test file and randomly scramble them
    public static void randomMutator(File inputFile, String filePath){
        String text = "";
        try
        {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line = br.readLine();
            while (line != null){
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            text = sb.toString();
            br.close();
            
        }

        catch (IOException e){
            e.printStackTrace();
        }


        StringBuilder sb2 = new StringBuilder();

        for(int i=0; i<text.length();i++){
            byte[] array = new byte[1]; // length is bounded by 1
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));
        
                //replace with random alphanumeric char 

                if (text.charAt(i) == '\n' || text.charAt(i) == ' '){
                    sb2.append(text.charAt(i));
                }

                else {
                    sb2.append(generatedString);
                }    

        }

        String textTwo = sb2.toString();

        Random random = new Random();
        try {
            //Generate a random file number
            int fileNum = random.nextInt(10000);
            File cnfFile = new File(filePath);
            cnfFile.createNewFile();
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(textTwo);
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    //A method which takes in an input file and for each existing integer, replaces it with a random integer
    //It does this by using the existing integer as a seed
    public static void randomInts(File inputFile, String filePath){
        String text = "";
        try
        {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line = br.readLine();
            while (line != null){
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            text = sb.toString();
            br.close();
            
        }

        catch (IOException e){
            e.printStackTrace();
        }
        StringBuilder sb2 = new StringBuilder();
        Random random = new Random();

        //Go through all ints and assign a random value
        int number; //The existing number
        int randomNumber;
        
        for (String word : text.split("\\s+")) {
            if (word.equals("p") || word.equals("cnf")){
                sb2.append(word + " ");
            }

            else if (word.equals("0")){
                sb2.append(word + "\n");
            }

            else {
                //Generate a new random integer by using the existing integer as a seed.
                number = Integer.parseInt(word);
                randomNumber = random.nextInt(Math.abs(number));

                if (number < 0){
                    randomNumber = randomNumber * -1;
                }

                sb2.append(randomNumber + " ");
            }
            
        }

        String textTwo = sb2.toString();
        
        try {
            //Generate a random file number
            int fileNum = random.nextInt(10000);
            File cnfFile = new File(filePath);
            cnfFile.createNewFile();
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(textTwo);
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    

    //Mutates the input by dropping random characters
    //Uses the input variable chanceOfMutation as the probability which this mutation occurs
    public static void randomDrop(File inputFile, String filePath, double chanceOfMutation){
        String text = "";
        try
        {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line = br.readLine();
            while (line != null){
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            text = sb.toString();
            br.close();
            
        }

        catch (IOException e){
            e.printStackTrace();
        }
        StringBuilder sb2 = new StringBuilder();

        //Go through all words and drop random ones
        for (String word : text.split("\\s+")) {
            if (word.equals("p") || word.equals("cnf")){
                sb2.append(word + " ");
            }

            else if (word.equals("0") || word.equals(" ") || word.equals("\n")){
                sb2.append(word + "\n");
            }

           else {
                    //Drop random words with the probability chanceOfMutation
                    double mutate = Math.random();
                    if (!(mutate < chanceOfMutation)){
                        //don't drop
                        sb2.append(word + " ");
                    }
            }    
            
        }

        
        String textTwo = sb2.toString();
        Random random = new Random();
        try {
            //Generate a random file number
            int fileNum = random.nextInt(10000);
            File cnfFile = new File(filePath);
            cnfFile.createNewFile();
            FileWriter outWriter = new FileWriter(filePath);
            outWriter.write(textTwo);
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}