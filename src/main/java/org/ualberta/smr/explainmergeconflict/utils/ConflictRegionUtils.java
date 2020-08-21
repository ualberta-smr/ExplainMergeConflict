package org.ualberta.smr.explainmergeconflict.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.ualberta.smr.explainmergeconflict.*;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConflictRegionUtils {

    /**
     * Verifies if a file (that is also a valid key for the conflict file hashmap for {@link MergeConflictService}) has
     * conflict regions. The function will fail if the file is not registered as a valid key for MergeConflictService's
     * conflict file hashmap.
     * @param file conflict file
     * @return true if conflict regions are initialized for the current file under MergeConflictService; otherwise false
     */
    public static boolean isConflictFileValid(@NotNull Project project, @NotNull VirtualFile file) {
        HashMap<String, ConflictFile> conflictFiles = MergeConflictService.getInstance(project).getConflictFiles();
        String key = file.getPath();
        assert conflictFiles.containsKey(key);
        List<ConflictRegion> conflictRegions = conflictFiles.get(key).getConflictRegions();

        // TODO - make sure if has subregions

        return !conflictRegions.isEmpty();
    }

    /**
     * Parses through the git diff output from the git handler and gets the appropriate start line numbers and length
     * data for p1, p2, and the entire conflict region
     *
     * Example:
     *
     * <<<<<<< HEAD           <-- line 1 of conflict region
     * hello                  <-- p1
     * =======
     * hello world            <-- p2
     * >>>>>>> conflictBranch <-- line 5 of conflict region
     *
     * Input = "@@@ -1,1 -1,1 +1,5 @@@"
     * conflict region pair = [1,5]
     * p1 pair = [1,1]
     * p2 pair = [1,1]
     * Return = [[1,5], [1,1], [1,1]]
     *
     * @param region conflict region line data in output
     * @return {@link Triple} instance containing conflict region pair, p1 pair, and p2 pair
     * (each as {@link Pair}<Integer, Integer> respectively)
     */
    public static Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> parseAndFindPairsForConflictRegion(@NotNull String region) {
        Pair<Integer, Integer> regionPair = null;
        Pair<Integer, Integer> p1Pair = null;
        Pair<Integer, Integer> p2Pair = null;
        Pattern pattern;
        Matcher matcher;

        // Get region pair
        pattern = Pattern.compile("\\d+,\\d+\\s@");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            String pair = matcher.group()
                    .replaceAll("\\s@", "")
                    .trim();
            regionPair = convertPair(pair);
        }
        assert regionPair != null;

        // Get p1 pair
        pattern = Pattern.compile("@\\s[-+]\\d+,\\d+");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            String pair = matcher.group()
                    .replaceAll("@\\s[-+]", "")
                    .trim();
            p1Pair = convertPair(pair);
        }
        assert p1Pair != null;

        // Get p2 pair
        pattern = Pattern.compile("\\d\\s[-+]\\d+,\\d+\\s[-+]");
        matcher = pattern.matcher(region);

        if (matcher.find()) {
            String pair  = matcher.group()
                    .replaceAll("^\\d\\s[-+]", "") // "x,"
                    .replaceAll("\\s[-+]$", "") // "y"
                    .trim();
            p2Pair = convertPair(pair);
        }
        assert p2Pair != null;

        return new Triple<>(regionPair, p1Pair, p2Pair);
    }

    /**
     * Converts a String pair "x,y" into an ArrayList<Integer> [x,y] to be used by the descriptor and other functions
     * for determining the position of the (sub)conflict region in the code editor.
     *
     * x = start line number of the (sub)conflict region
     * y = of the (sub)conflict region
     *
     * @param pair String formatted as "x,y" where x is the startLine of the conflict region and y is its length
     * @return an {@link Pair}<Integer, Integer> containing the conflict region startLine as an integer on index 0,
     * and the conflict region length as an integer on index 1
     */
    public static Pair<Integer, Integer> convertPair(@NotNull String pair) {
        int startLine = 0;
        int length = 0;
        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile("^\\d+,");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String startLineStr = matcher.group().replaceAll(",", "").trim();
            startLine = Integer.parseInt(startLineStr);
        } else {
            return null;
        }

        pattern = Pattern.compile(",\\d+$");
        matcher = pattern.matcher(pair);

        if (matcher.find()) {
            String lengthStr = matcher.group().replaceAll(",", "").trim();
            length = Integer.parseInt(lengthStr);
        } else {
            return null;
        }

        return new Pair<>(startLine, length);
    }
}
