package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.sun.istack.NotNull;
import org.ualberta.smr.explainmergeconflict.services.ExplainMergeConflictBundle;

import javax.swing.*;
import java.awt.*;

public class ExplanationsToolWindow {
    private ToolWindow toolWindow;
    private JPanel explanationsToolWindowContent;
    private JPanel headerPanel;
    private JTextPane headerTextPane;
    private JTextPane textPaneOurs;
    private JTextPane textPaneTheirs;
    private JButton showLogButton;
    private JButton showLogButton1;
    private JLabel labelOurs;
    private JLabel labelTheirs;
    private JScrollPane headerPane;

    public ExplanationsToolWindow(@NotNull Project project,
                                  @NotNull ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        initializeToolWindow();
    }

    private void initializeToolWindow() {
        toolWindow.setAutoHide(true);
        headerPane.setToolTipText(ExplainMergeConflictBundle.message("toolwindow.tooltip.description"));
        headerTextPane.setText("<NUMBER> conflict regions were found in file " +
                "<FILENAME> while attempting to to merge <THEIR BRANCH> into " +
                "<OUR BRANCH>.\n\nALL SUSPICIOUS COMMITS ARE MARKED IN RED.");
//        explanationsToolWindowContent.setBorder(UIUtil.getTextFieldBorder());
        updateBackgroundColors();
    }

    private void updateBackgroundColors() {
        // TODO update color when Editor theme changes
//        headerTextPane.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
//        textPaneOurs.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
//        textPaneTheirs.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
    }

    JPanel getContent() {
        return explanationsToolWindowContent;
    }
}
