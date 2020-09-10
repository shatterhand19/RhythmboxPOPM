import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Bozhidar Ganev on 12.12.17.
 */
public class POPMDecoder {
    /**
     * Extracts the rating from the popularimeter frame.
     * The format of the exiftool output is:
     * Popularimeter                   : <Email> Rating=0 Count=0
     *
     * @param popm is the exiftool output for the POPM frame.
     * @return the extracted rating.
     */
    public static int extractRating(String popm) {
        if (popm == null) return 0;

        String pattern = "Rating=(\\d+)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(popm);
        if (!m.find()) return 0;
        int rating = Integer.parseInt(m.group(1));

        /*
        If mapping from a 5 star rating system, we would have (rounded down)...

        1 => 1/5*255 = 51
        2 => 2/5*255 = 102
        3 => 3/5*255 = 153
        4 => 4/5*255 = 204
        5 => 5/5*255 = 255

        Gap is about 50
        Half gap is 25, and should be our tolerance
        */

        if (rating == 0) {
            return 0;
        }
        if (rating <= 51 + 25) {
            return 1;
        }
        if (rating <= 102 + 25) {
            return 2;
        }
        if (rating <= 153 + 25) {
            return 3;
        }
        if (rating <= 204 + 25) {
            return 4;
        }
        return 5;
    }

    public static int extractPlayCount(String popm) {
        if (popm == null) return 0;

        String pattern = "Count=(\\d+)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(popm);
        if (!m.find()) return 0;
        return Integer.parseInt(m.group(1));
    }
}
