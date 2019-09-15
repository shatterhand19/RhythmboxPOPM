import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by bozhidar on 11.10.17.
 */
public class RBToPOPM {
    public static HashMap<Integer, Integer> starsToNumber = new HashMap<>();
    static {
        starsToNumber.putIfAbsent(0, 0);
        starsToNumber.putIfAbsent(1, 51);
        starsToNumber.putIfAbsent(2, 102);
        starsToNumber.putIfAbsent(3, 153);
        starsToNumber.putIfAbsent(4, 204);
        starsToNumber.putIfAbsent(5, 255);
    }

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";
    private static int total = 0;
    private static String email = null;

    public static void main(String... args) throws ParserConfigurationException, IOException, SAXException, URISyntaxException, InterruptedException, ExecutionException {
        //Read flags
        if (args.length >= 2) {
            email = args[1];
        } else {
            email = "someone@example.com";
        }

        //Enumerate all the files that are passed to the program
        List<String> filenames = new ArrayList<>();
        if (args.length >= 1) {
            if (!new File(args[0]).isDirectory()) {
                filenames = Files.readAllLines(Paths.get(args[0]));
            } else {
                System.out.println(ANSI_RED + "Song file argument specified is a directory (first argument should be the path to a file containing filenames)" + ANSI_RESET);
                return;
            }
        } else {
            System.out.println(ANSI_RED + "No song file argument specified (first argument should be the path to a file containing filenames)" + ANSI_RESET);
            return;
        }
        HashMap<String, File> files = new HashMap<>();
        for (String filename : filenames) {
            if (!filename.equals("")) files.put(filename, new File(filename));
        }

        String database = System.getProperty("user.home") + "/.local/share/rhythmbox/rhythmdb.xml";
        int threads = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        System.out.println("Running program on " + threads + " threads");

        File db = new File(database);
        if (db.canRead()) {
            //Read the RB database
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(db);

            doc.getDocumentElement().normalize();

            //For all songs
            NodeList songs = doc.getElementsByTagName("entry");
            for (int i = 0; i < songs.getLength(); i++) {
                Node song = songs.item(i);
                Element songElement = (Element) song;
                Node location = songElement.getElementsByTagName("location").item(0);
                if (location != null) {
                    //Get the filename and if it is one of the requested files
                    String val = location.getTextContent();
                    val = val.replace("file://", "");
                    val = URLDecoder.decode(val, "UTF-8");
                    if (files.containsKey(val)) {
                        File toAddPOPM = files.get(val);
                        boolean fileUpdated = false;
                        int popmRating = 0;
                        int playCount = 0;

                        NodeList rbRatingNodeList = songElement.getElementsByTagName("rating");
                        if (rbRatingNodeList != null) {
                            //Get the rating of Rhythmbox
                            int rbRating;
                            Node rbRatingNode = rbRatingNodeList.item(0);
                            if (rbRatingNode == null) {
                                rbRating = 0;
                            } else {
                                rbRating = Integer.parseInt(rbRatingNode.getTextContent());
                            }
                            popmRating = starsToNumber.get(rbRating);

                            fileUpdated = true;
                        }

                        NodeList rbPlayCountNodeList = songElement.getElementsByTagName("play-count");
                        if (rbPlayCountNodeList != null) {
                            //Get the play count of Rhythmbox
                            Node rbPlayCountNode = rbPlayCountNodeList.item(0);
                            if (rbPlayCountNode == null) {
                                playCount = 0;
                            } else {
                                playCount = Integer.parseInt(rbPlayCountNode.getTextContent());
                            }

                            fileUpdated = true;
                        }

                        if (fileUpdated) {
                            // Save the change
                            final int popmRatingFinal = popmRating;
                            final int playCountFinal = playCount;
                            executor.execute(() -> {
                                try {
                                    addNewPOPM(toAddPOPM, popmRatingFinal, playCountFinal);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });

                            //Increment the number of total processed files
                            total++;
                        }
                    }
                }
            }
            executor.shutdown();
        }

        System.out.println("\n" + ANSI_GREEN + "Processed " + total + " songs successfully!" + ANSI_RESET + "");
    }

    public static void addNewPOPM(File toAddPOPM, int popmRating, int playCount) throws IOException, InterruptedException {
        String options = "--add-popularity=" + email + ":" + popmRating + ":" + playCount;

        /*boolean dryRunDebug = true;
        if (dryRunDebug) {
            System.out.println(options);
            return;
        }*/

        //Clear out old rating
        System.out.println(ANSI_YELLOW + "[Deleting obsolete POPM]\t" + ANSI_RESET + toAddPOPM.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder("eyeD3", "--remove-frame=POPM", toAddPOPM.getAbsolutePath());
        Process p = pb.start();
        p.waitFor();

        //Add the rating
        System.out.println(ANSI_GREEN + "[Adding POPM] [Rating=" + popmRating + ", Play count=" + playCount + "]\t" + ANSI_RESET + toAddPOPM.getAbsolutePath());
        pb = new ProcessBuilder("eyeD3", options, toAddPOPM.getAbsolutePath());
        Process addRating = pb.start();
        addRating.waitFor();
    }
}
