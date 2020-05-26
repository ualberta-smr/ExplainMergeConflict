package ui;

import com.intellij.diff.DiffContentFactoryEx;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.vcs.log.VcsCommitMetadata;
import com.intellij.vcs.log.ui.MainVcsLogUi;
import com.intellij.vcs.log.ui.VcsLogInternalDataKeys;
import com.intellij.vcs.log.ui.frame.VcsLogChangesBrowser;
import com.intellij.vcs.log.ui.table.VcsLogGraphTable;
import data.RefactoringEntry;
import data.RefactoringInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NotNull;
import services.MiningService;
import services.RefactoringsBundle;

public class GitWindow extends ToggleAction {

  Project project;
  AnActionEvent event;
  DiffContentFactoryEx myDiffContentFactory;
  private ChangesTree changesTree;
  private JBViewport viewport;
  private boolean selected = false;
  private VcsLogGraphTable table;
  private JBScrollPane scrollPane;
  private MiningService miningService;

  private void setUp(@NotNull AnActionEvent e) {
    VcsLogChangesBrowser changesBrowser =
        (VcsLogChangesBrowser) e.getData(VcsLogChangesBrowser.DATA_KEY);
    changesTree = changesBrowser.getViewer();
    MainVcsLogUi logUI = e.getData(VcsLogInternalDataKeys.MAIN_UI);

    project = e.getProject();
    miningService = project.getService(MiningService.class);

    table = logUI.getTable();
    table.getSelectionModel().addListSelectionListener(new CommitSelectionListener());

    event = e;
    myDiffContentFactory = DiffContentFactoryEx.getInstanceEx();
    viewport = (JBViewport) changesTree.getParent();
    scrollPane = new JBScrollPane(new JBLabel(RefactoringsBundle.message("not.selected")));
  }

  private void toRefactoringView(@NotNull AnActionEvent e) {
    while (miningService.isMining()) {

    }
    int index = table.getSelectionModel().getAnchorSelectionIndex();
    if (index != -1) {
      buildComponent(index);
    }
    viewport.setView(scrollPane);
  }

  private void toChangesView(@NotNull AnActionEvent e) {
    viewport.setView(changesTree);
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return selected;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    if (changesTree == null) {
      setUp(e);
    }
    if (state) {
      toRefactoringView(e);
    } else {
      toChangesView(e);
    }
    selected = state;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(true);
    super.update(e);
  }

  /**
   * Method called after a single commit is mined.
   * Updates the view with the refactorings found.
   *
   * @param commitId to refresh the view at.
   */
  public void refresh(String commitId) {
    int index = table.getSelectionModel().getAnchorSelectionIndex();
    if (table.getModel().getCommitId(index).getHash().asString().equals(commitId)) {
      buildComponent(index);
    }
  }

  private void buildComponent(int index) {
    String commitId = table.getModel().getCommitId(index).getHash().asString();
    VcsCommitMetadata metadata = table.getModel().getCommitMetadata(index);

    String refactorings = miningService.getRefactorings(commitId);
    RefactoringEntry entry = RefactoringEntry.fromString(refactorings);

    if (entry != null) {
      Tree tree = entry.buildTree();
      tree.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path == null) {
              return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                path.getLastPathComponent();
            if (node.isLeaf()) {
              RefactoringInfo info = (RefactoringInfo)
                  node.getUserObjectPath()[1];
              showDiff(index, info);
            }
          }
        }
      });
      scrollPane.getViewport().setView(tree);
    } else {
      JBLabel label = new JBLabel(RefactoringsBundle.message("not.mined"));

      JBPanel panel = new JBPanel();
      JButton button = new JButton("Mine this commit");
      GitWindow gitWindow = this;
      button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          miningService
              .mineAtCommit(metadata, project, gitWindow);
        }
      });
      panel.add(button);

      JBSplitter splitter = new JBSplitter(true, (float) 0.1);
      splitter.setFirstComponent(label);
      splitter.setSecondComponent(panel);
      scrollPane.getViewport().setView(splitter);
    }
  }

  private void showDiff(int index, RefactoringInfo info) {
    try {
      Collection<Change> changes = table.getModel().getFullDetails(index).getChanges(0);

      String left = "";
      String right = "";
      for (Change change : changes) {
        if (change.getBeforeRevision() != null
            && (project.getBasePath() + "/" + info.getLeftPath())
            .equals(change.getBeforeRevision().getFile().getPath())) {
          left = change.getBeforeRevision().getContent();
        }
        if (change.getAfterRevision() != null
            && (project.getBasePath() + "/" + info.getRightPath())
            .equals(change.getAfterRevision().getFile().getPath())) {
          right = change.getAfterRevision().getContent();
        }
      }
      String mid = "";
      if (info.isThreeSided()) {
        for (Change change : changes) {
          if (change.getAfterRevision() != null
              && (project.getBasePath() + "/" + info.getMidPath())
              .equals(change.getAfterRevision().getFile().getPath())) {
            mid = change.getAfterRevision().getContent();
          }
        }
        DiffWindow.showDiff(left, mid, right, info, project);
      } else {
        DiffWindow.showDiff(left, right, info, project);
      }

    } catch (VcsException e) {
      e.printStackTrace();
    }
  }

  class CommitSelectionListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
      if (listSelectionEvent.getValueIsAdjusting()) {
        return;
      }
      DefaultListSelectionModel selectionModel =
          (DefaultListSelectionModel) listSelectionEvent.getSource();

      int beginIndex = selectionModel.getMinSelectionIndex();
      int endIndex = selectionModel.getMaxSelectionIndex();

      if (beginIndex != -1 || endIndex != -1) {
        if (!miningService.isMining()) {
          buildComponent(beginIndex);
        }
      }
    }
  }
}