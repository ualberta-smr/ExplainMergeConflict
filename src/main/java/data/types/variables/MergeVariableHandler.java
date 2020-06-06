package data.types.variables;

import com.intellij.openapi.project.Project;
import data.Group;
import data.RefactoringInfo;
import data.types.Handler;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import java.util.stream.Collectors;
import org.refactoringminer.api.Refactoring;

public class MergeVariableHandler extends Handler {

  @Override
  public RefactoringInfo specify(Refactoring refactoring, RefactoringInfo info, Project project) {
    MergeVariableRefactoring ref = (MergeVariableRefactoring) refactoring;

    ref.getMergedVariables().forEach(var ->
        info.addMarking(var.codeRange(), ref.getNewVariable().codeRange()));

    if (ref.getNewVariable().isParameter()) {
      info.setGroup(Group.PARAMETER);
    } else {
      info.setGroup(Group.VARIABLE);
    }

    return info
        .setElementBefore(ref.getMergedVariables().stream().map(x -> x.getVariableName()).collect(
            Collectors.joining()))
        .setElementAfter(ref.getNewVariable().toQualifiedString())
        .setNameBefore(ref.getNewVariable().toQualifiedString())
        .setNameAfter(ref.getNewVariable().toQualifiedString());
  }
}