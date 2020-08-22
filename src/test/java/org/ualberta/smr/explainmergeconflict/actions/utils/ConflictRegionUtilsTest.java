package org.ualberta.smr.explainmergeconflict.actions.utils;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Triple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.ConflictRegion;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.utils.ConflictRegionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConflictRegionUtilsTest {
    private MergeConflictService mockMergeConflictService;
    private Project mockProject;
    private ConflictFile mockConflictFile;
    private ConflictRegion mockConflictRegion;
    private VirtualFile mockFile;
    HashMap<String, ConflictFile> mockConflictFiles = new HashMap<>();
    List<ConflictRegion> mockConflictRegions = new ArrayList<>();

    @Before
    public void setUp() {
        mockMergeConflictService = mock(MergeConflictService.class, RETURNS_DEEP_STUBS);
        mockProject = mock(Project.class, RETURNS_DEEP_STUBS);
        mockConflictFile = mock(ConflictFile.class);
        mockConflictRegion = mock(ConflictRegion.class);
        mockFile = mock(VirtualFile.class);

        mockConflictFiles.put("file1", mockConflictFile);
        mockConflictFiles.put("file2", mockConflictFile);

        when(MergeConflictService.getInstance(mockProject)).thenReturn(mockMergeConflictService);
        when(mockMergeConflictService.getConflictFiles()).thenReturn(mockConflictFiles);

        mockConflictRegions.add(mockConflictRegion);
        when(mockConflictFiles.get("file1").getConflictRegions()).thenReturn(mockConflictRegions);
        when(mockConflictFiles.get("file2").getConflictRegions()).thenReturn(mockConflictRegions);
    }

    @After
    public void tearDown() {
        mockConflictFiles.clear();
        mockConflictRegions.clear();
    }

    @Test
    public void shouldReturnTrueIfConflictFileValid() {
        when(mockFile.getPath()).thenReturn("file1");
        boolean result = ConflictRegionUtils.isConflictFileValid(mockProject, mockFile);
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseIfConflictFileInValid() {
        when(mockFile.getPath()).thenReturn("file1");
        when(mockConflictFiles.get("file1").getConflictRegions()).thenReturn(new ArrayList<>());
        boolean result = ConflictRegionUtils.isConflictFileValid(mockProject, mockFile);
        assertFalse(result);
    }

    @Test
    public void parseAndFindPairsForConflictRegionTest() {
        Pair<Integer, Integer> conflictPair;
        Pair<Integer, Integer> p1Pair;
        Pair<Integer, Integer> p2Pair;

        // Hunk data might not make sense semantically, but we will just use these line values for testing reasons
        String region = "@@@ -100,45 -1,4000 +1,52 @@@";
        conflictPair = new Pair<>(1,52);
        p1Pair = new Pair<>(100,45);
        p2Pair = new Pair<>(1,4000);
        Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> result =
                ConflictRegionUtils.parseAndFindPairsForConflictRegion(region);
        assertNotNull(result);
        assertEquals(conflictPair, result.getFirst());
        assertEquals(p1Pair, result.getSecond());
        assertEquals(p2Pair, result.getThird());
    }

    @Test
    public void convertPairTest() {
        String pair;
        Pair<Integer, Integer> result;

        pair = "1,1";
        result = ConflictRegionUtils.convertPair(pair);
        assertThat(result, instanceOf(Pair.class));
        assertEquals(1, (int) result.getFirst());
        assertEquals(1, (int) result.getSecond());

        pair = "100,2000";
        result = ConflictRegionUtils.convertPair(pair);
        assertEquals(100, (int) result.getFirst());
        assertEquals(2000, (int) result.getSecond());

        pair = "342323m34ji324,sjiofdjiodfs";
        result = ConflictRegionUtils.convertPair(pair);
        assertNull(result);

        pair = "123,";
        result = ConflictRegionUtils.convertPair(pair);
        assertNull(result);

        pair = ",7879978";
        result = ConflictRegionUtils.convertPair(pair);
        assertNull(result);
    }
}
