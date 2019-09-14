import java.util.HashMap;

/**
 * Created by Bozhidar Ganev on 12.12.17.
 */
public class Rating {
    public static HashMap<Integer, Integer> numberToStars = new HashMap<>();
    static {
        numberToStars.put(0, 0);
        numberToStars.put(1, 1);
        numberToStars.put(64, 2);
        numberToStars.put(128, 3);
        numberToStars.put(196, 4);
        numberToStars.put(255, 5);
    }
    /**
     * Extracts the rating from the popularimeter frame.
     * The format of the exiftool output is:
     * Popularimeter                   : <Email> Rating=0 Count=0
     *
     * @param popm is the exiftool output for the POPM frame.
     * @return the extracted rating.
     */
    public static int extractRating(String popm) {
        if (popm.equals("-")) return 0;
        String ratingPart = popm;
        if (ratingPart.contains(":")) ratingPart = popm.split(":")[1].trim();
        if (!ratingPart.contains("Rating=")) return 0;
        ratingPart = ratingPart.substring(ratingPart.indexOf("Rating="));
        ratingPart = ratingPart.split(" ")[0];
        int rating = Integer.parseInt(ratingPart.split("=")[1]);
        if (rating > 5) {
            rating = numberToStars.getOrDefault(rating, rating);
        }
        return rating;
    }
}
