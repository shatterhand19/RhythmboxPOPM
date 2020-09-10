import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Bozhidar Ganev on 13.12.17.
 *
 * Class that helps reading the information from the ID3 tags
 */
public class ID3Reader {
    /**
     * Reads the ID3 frames of an array of files.
     * It uses exiftool and makes a process that executes and returns
     * the output from exiftool for these files and the requested frames.
     *
     * @param files are the global filenames of the files
     * @param frames are the requested frames
     * @return a 2D array with the frames
     * @throws IOException if there is a problem reading the output of the exiftool command
     * @throws InterruptedException if the exiftool process is interrupted
     */
    static String[][] getFrames(String[] files, String... frames) throws IOException, InterruptedException {
        //Create the command
        ArrayList<String> options = new ArrayList<>();
        options.add("exiftool");
        //Make it force print if a frame does not exist
        options.add("-f");
        //Add the requested frames
        options.addAll(Arrays.asList(frames));
        //Add the requested files
        options.addAll(Arrays.asList(files));

        //Build the process
        ProcessBuilder pc = new ProcessBuilder(options.toArray(new String[options.size()]));
        Process p = pc.start();
        p.waitFor();

        //Read the output
        BufferedReader processOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder result = new StringBuilder("");
        String line = null;
        int l = 0;
        while ((line = processOutput.readLine()) != null) {
            result.append(line).append(System.lineSeparator());
        }

        //Create a way to fast lookup filenames back to our file indices
        HashMap<String, Integer> filesLookup = new HashMap<>();
        for (var i = 0; i < files.length; i++) {
            filesLookup.put(files[i], i);
        }

        //Parse the information in the frames
        String[][] fileFrames = new String[files.length][frames.length];
        Scanner scanner = new Scanner(result.toString());
        Pattern patternFilename = Pattern.compile("^=+ (.*)$");
        Pattern patternColonedResult = Pattern.compile("^.*\\s+: (.*)$");
        String currentFilename = null;
        int currentFrameIndex = 0;
        Matcher patternFilenameMatcher = null;
        Matcher patternColonedResultMatcher = null;
        String nextLine;
        int i;
        while (scanner.hasNext()) {
            nextLine = scanner.nextLine();

            patternFilenameMatcher = patternFilename.matcher(nextLine);
            patternColonedResultMatcher = patternColonedResult.matcher(nextLine);

            if (patternFilenameMatcher.find()) {
                currentFilename = patternFilenameMatcher.group(1);
                currentFrameIndex = 0;
            } else if ((currentFilename != null) && (currentFrameIndex < frames.length)) {
                i = filesLookup.get(currentFilename);

                if (patternColonedResultMatcher.find()) {
                    //If the frame has ":" (typically when the frame exists), get only the meaningful part
                    fileFrames[i][currentFrameIndex] = patternColonedResultMatcher.group(1);
                    currentFrameIndex++;
                } else {
                    fileFrames[i][currentFrameIndex] = nextLine;
                    currentFrameIndex++;
                }
            }
        }

        return fileFrames;
    }

    /**
     * Reads the ID3 frames of an array of files.
     * It uses exiftool and makes a process that executes and returns
     * the output from exiftool for these files and the requested frames.
     *
     * @param files are the files
     * @param frames are the requested frames
     * @return a 2D array with the frames
     * @throws IOException if there is a problem reading the output of the exiftool command
     * @throws InterruptedException if the exiftool process is interrupted
     */
    static String[][] getFrames(File[] files, String... frames) throws IOException, InterruptedException {
        String[] filenames = new String[files.length];
        //Fill in the filenames array
        for (int i = 0; i < files.length; i++) {
            filenames[i] = files[i].getAbsolutePath();
        }

        return parallelGetFrame(filenames, frames);
    }

    static String[][] parallelGetFrame(String[] files, String... frames) throws IOException, InterruptedException {
        int batchSize = 200;
        String[][] result = new String[files.length][frames.length];
        if (files.length <= batchSize) return getFrames(files, frames);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);
        for (int i = 0; i < files.length / batchSize ; i++) {
            int start = i * batchSize;
            String[] filesBatch = new String[batchSize];
            System.arraycopy(files, start, filesBatch, 0, batchSize);
            executor.execute(() -> {
                try {
                    String[][] partial = getFrames(filesBatch, frames);
                    System.arraycopy(partial, 0, result, start, batchSize);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        if (files.length % batchSize != 0) {
            int start = files.length - files.length % batchSize;
            String[] filesBatch = new String[files.length % batchSize];
            System.arraycopy(files, start, filesBatch, 0, files.length % batchSize);
            executor.execute(() -> {
                try {
                    String[][] partial = getFrames(filesBatch, frames);
                    System.arraycopy(partial, 0, result, start, files.length % batchSize);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        return result;
    }
}
