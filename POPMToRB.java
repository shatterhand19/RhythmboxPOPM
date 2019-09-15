import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by Bozhidar Ganev on 25.12.17.
 */
public class POPMToRB {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";
    private static boolean overwrite = false;
    private static boolean promptChange = true;
    private static int total = 0;

    public static void main(String... args) throws ParserConfigurationException, IOException, SAXException, URISyntaxException, InterruptedException, TransformerException {
        //Read flags
        if (args.length >= 2 && args[1].equals("-f")) {
            promptChange = false;
        }
        if (args.length >= 2 && args[1].equals("-o")) {
            overwrite = true;
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
        System.out.println(ANSI_YELLOW + "Found " + files.size() + " files, now reading rating flags..." + ANSI_RESET);
        File[] filesArray = files.values().toArray(new File[files.size()]);

        //Read the ratings of the music files
        String[][] ratingsFrames = ID3Reader.getFrames(filesArray, "-popularimeter");
        HashMap<String, Integer> ratings = new HashMap<>();
        int numFlags = 0;
        for (int i = 0; i < filesArray.length; i++) {
            int fileRating = Rating.extractRating(ratingsFrames[i][0]);
            ratings.put(filesArray[i].getAbsolutePath(), fileRating);
            if (fileRating != 0) numFlags++;
        }

        System.out.println(ANSI_YELLOW + "Found " + numFlags + " flags, now processing into Rhythmbox..." + ANSI_RESET);

        //Go through the RhythmBox XML file
        String database = System.getProperty("user.home") + "/.local/share/rhythmbox/rhythmdb.xml";
        File db = new File(database);
        if (db.canRead()) {
            //Read the RB database
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(db);

            doc.getDocumentElement().normalize();

            //For all songs
            NodeList songs = doc.getElementsByTagName("entry");
            int totalEntries = songs.getLength();
            for (int i = 0; i < totalEntries; i++) {
                Node song = songs.item(i);
                Element songElement = (Element) song;
                Node location = songElement.getElementsByTagName("location").item(0);
                //If the location is not null
                if (location != null) {
                    String val = location.getTextContent();
                    val = val.replace("file://", "");
                    val = URLDecoder.decode(val, "UTF-8");
                    //Get the filename and if it is one of the requested files
                    if (files.containsKey(val)) {
                        NodeList temp = songElement.getElementsByTagName("rating");
                        if (temp != null) {
                            //Get the rating of Rhythmbox
                            int rbRating, fileRating = ratings.get(val);
                            Node stars = temp.item(0);
                            if (fileRating > 0) {
                                String progressStub = (i + 1) + "/" + totalEntries + ") ";
                                if (stars == null) {
                                    addNewRating(doc, songElement, ratings.get(val));
                                    System.out.println(progressStub + ANSI_GREEN + "Rating of " + ANSI_RESET + val + ANSI_GREEN + " set to " + ANSI_RESET + ratings.get(val) + "\n");
                                } else {
                                    String existsMsg = progressStub + "Rating for " + ANSI_YELLOW + val + ANSI_RED + " already exists";
                                    rbRating = Integer.parseInt(temp.item(0).getTextContent());
                                    if (fileRating != rbRating && overwrite) {
                                        addNewRating(doc, songElement, fileRating);
                                        System.out.println(ANSI_GREEN + "Rating of " + ANSI_RESET + val + ANSI_GREEN + " changed to " + ANSI_RESET + ratings.get(val) + "\n");
                                    } else if (fileRating != rbRating && promptChange) {
                                        System.out.println(existsMsg + "!" + ANSI_RESET);
                                        System.out.println(ANSI_GREEN + "Rhythmbox rating " + ANSI_RESET + rbRating + ANSI_YELLOW + "    File rating " + ANSI_RESET + fileRating);
                                        System.out.println("Do you want to override it? (Y/n)");
                                        Scanner response = new Scanner(System.in);
                                        String ans = response.next();
                                        if (ans.equals("Y")) {
                                            addNewRating(doc, songElement, fileRating);
                                            System.out.println(ANSI_GREEN + "Rating of " + ANSI_RESET + val + ANSI_GREEN + " changed to " + ANSI_RESET + ratings.get(val) + "\n");
                                        }
                                    } else if (fileRating == rbRating) {
                                        System.out.println(existsMsg + ", rating is unchanged, skipping!" + ANSI_RESET);
                                    } else {
                                        System.out.println(existsMsg + ", overwriting is disabled due to -f, skipping!" + ANSI_RESET);
                                    }
                                }

                                total++;
                            } else {
                                System.out.println(ANSI_RED + "Could not find any rating set in " + val + ANSI_RESET + "\n");
                            }
                        }
                    }
                }
            }

            System.out.println(ANSI_YELLOW + "Writing to database" + ANSI_RESET);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(database));
            transformer.transform(source, result);
            System.out.println(ANSI_GREEN + "Database written!" + ANSI_RESET);
        }
        System.out.println("\n" + ANSI_GREEN + "Processed " + total + " songs successfully!" + ANSI_RESET + "");
    }

    public static void addNewRating(Document document, Element song, int rating) {
        Element r = document.createElement("rating");
        r.appendChild(document.createTextNode(Integer.toString(rating)));
        song.appendChild(r);
    }
}
