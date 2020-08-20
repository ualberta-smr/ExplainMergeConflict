package org.ualberta.smr.explainmergeconflict.actions.utils;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.ualberta.smr.explainmergeconflict.ConflictFile;
import org.ualberta.smr.explainmergeconflict.services.MergeConflictService;
import org.ualberta.smr.explainmergeconflict.utils.Utils;

import java.util.HashMap;

public class UtilsTest {
    private MergeConflictService mockMergeConflictService;
    private Project mockProject;
    private ConflictFile mockConflictFile;
    private VirtualFile mockFile;

    @Before
    public void setUp(){
        mockMergeConflictService = mock(MergeConflictService.class, RETURNS_DEEP_STUBS);
        mockProject = mock(Project.class, RETURNS_DEEP_STUBS);
        mockConflictFile = mock(ConflictFile.class);
        mockFile = mock(VirtualFile.class);

        HashMap<String, ConflictFile> mockConflictFiles = new HashMap<>();
        mockConflictFiles.put("file1", mockConflictFile);
        mockConflictFiles.put("file2", mockConflictFile);

        when(MergeConflictService.getInstance(mockProject)).thenReturn(mockMergeConflictService);
        when(mockMergeConflictService.getConflictFiles()).thenReturn(mockConflictFiles);
    }

    @Test
    public void shouldReturnTrueIfConflictFile() {
        when(mockFile.getPath()).thenReturn("file1");
        boolean result = Utils.isConflictFile(mockProject, mockFile);
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseIfNotConflictFile() {
        when(mockFile.getPath()).thenReturn("notConflictFile");
        boolean result = Utils.isConflictFile(mockProject, mockFile);
        assertFalse(result);
    }
}
