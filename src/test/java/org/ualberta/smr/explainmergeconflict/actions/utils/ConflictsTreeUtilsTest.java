package org.ualberta.smr.explainmergeconflict.actions.utils;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.ConflictRegion;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNode;
import org.ualberta.smr.explainmergeconflict.ui.trees.renderers.ConflictNodeType;
import org.ualberta.smr.explainmergeconflict.utils.ConflictsTreeUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConflictsTreeUtilsTest {
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

        when(mockFile.getPath()).thenReturn("file1");

        mockConflictFiles.put("file1", mockConflictFile);
        mockConflictFiles.put("file2", mockConflictFile);

        when(MergeConflictService.getInstance(mockProject)).thenReturn(mockMergeConflictService);
        when(mockMergeConflictService.getConflictFiles()).thenReturn(mockConflictFiles);

        mockConflictRegions.add(mockConflictRegion);
        mockConflictRegions.add(mockConflictRegion);
        mockConflictRegions.add(mockConflictRegion);
        when(mockConflictFiles.get("file1").getConflictRegions()).thenReturn(mockConflictRegions);
    }

    @After
    public void tearDown() {
        mockConflictFiles.clear();
        mockConflictRegions.clear();
    }

    @Test
    public void createRootAndChildrenTest() {
        ConflictNode node = new ConflictNode(ConflictNodeType.BRANCHROOT, "Root");
        DefaultMutableTreeNode result = ConflictsTreeUtils.createRootAndChildren(mockProject, mockFile, node);
        assertNotNull(result);
        assertEquals(3, result.getChildCount());
    }
}
