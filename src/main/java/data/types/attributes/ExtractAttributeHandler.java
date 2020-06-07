package data.types.attributes;

import data.Group;
import data.RefactoringInfo;
import data.types.Handler;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import org.refactoringminer.api.Refactoring;
import utils.Utils;

public class ExtractAttributeHandler extends Handler {

  @Override
  public RefactoringInfo specify(Refactoring refactoring, RefactoringInfo info) {
    ExtractAttributeRefactoring ref = (ExtractAttributeRefactoring) refactoring;
    ref.leftSide().forEach(extraction ->
        info.addMarking(
            extraction,
            ref.getExtractedVariableDeclarationCodeRange(),
            true
        ));
    return info.setGroup(Group.ATTRIBUTE)
        .setElementBefore("in class " + ref.getOriginalClass().getName())
        .setElementAfter(null)
        .setNameBefore(ref.getVariableDeclaration().toQualifiedString())
        .setNameAfter(ref.getVariableDeclaration().toQualifiedString());
  }
}
