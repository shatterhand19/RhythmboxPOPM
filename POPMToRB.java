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
    private static boolean promptChange = true;

    public static void main(String... args) throws ParserConfigurationException, IOException, SAXException, URISyntaxException, InterruptedException, TransformerException {
        //Read flag
        if(args.length == 2 && args[1].equals("-f")) {
            promptChange = false;
        }
        args = Files.readAllLines(Paths.get(args[0])).get(0).split(",;");
        HashMap<String, File> files = new HashMap<>();
        HashMap<String, Integer> ratings = new HashMap<>();
        //Read all the files that are passed to the program
        for (String arg : args) {
            files.put(arg, new File(arg));
        }

        File[] filesArray = files.values().toArray(new File[files.size()]);

        //Read the ratings of the music files
        String database = System.getProperty("user.home") + "/.local/share/rhythmbox/rhythmdb.xml";
        String[][] ratingsFrames = ID3Reader.getFrames(filesArray, "-popularimeter");
        for (int i = 0; i < filesArray.length; i++) {
            ratings.put(filesArray[i].getAbsolutePath(), Rating.extractRating(ratingsFrames[i][0]));
        }

        File db = new File(database);
        if (db.canRead()) {
            //Read the RB database
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(db);

            doc.getDocumentElement().normalize();

            NodeList songs = doc.getElementsByTagName("entry");
            //For all songs
            for (int i = 0; i < songs.getLength(); i++) {
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
                            //Get the rating of RhythmBox
                            int RBRating, FileRating = ratings.get(val);
                            Node stars = temp.item(0);
                            if (FileRating > 0) {
                                if (stars == null) {
                                    addNewRating(doc, songElement, ratings.get(val));
                                    System.out.println(ANSI_GREEN + "Rating of " + ANSI_RESET + val + ANSI_GREEN + " set to " + ANSI_RESET + ratings.get(val) + "\n");
                                } else {
                                    System.out.println(ANSI_RED + "Rating for " + ANSI_YELLOW + val + ANSI_RED + " already exists!" + ANSI_RESET);
                                    RBRating = Integer.parseInt(temp.item(0).getTextContent());
                                    if (FileRating != RBRating && promptChange) {
                                        System.out.println(ANSI_GREEN + "Rhythmbox rating " + ANSI_RESET + RBRating + ANSI_YELLOW + "    File rating " + ANSI_RESET + FileRating);
                                        System.out.println("Do you want to override it? (Y/n)");
                                        Scanner response = new Scanner(System.in);
                                        String ans = response.next();
                                        if (ans.equals("Y")) {
                                            addNewRating(doc, songElement, FileRating);
                                            System.out.println(ANSI_GREEN + "Rating of " + ANSI_RESET + val + ANSI_GREEN + " set to " + ANSI_RESET + ratings.get(val) + "\n");
                                        }
                                    }
                                }
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
        //System.out.println("\n" + ANSI_GREEN + "Processed " + total + " songs successfully!" + ANSI_RESET + "");
    }

    public static void addNewRating(Document document, Element song, int rating) {
        Element r = document.createElement("rating");
        r.appendChild(document.createTextNode(Integer.toString(rating)));
        song.appendChild(r);
    }
}
