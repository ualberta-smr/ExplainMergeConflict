package org.jetbrains.research.refactorinsight.data.types.classes;

import gr.uom.java.xmi.diff.ModifyClassAnnotationRefactoring;
import org.jetbrains.research.refactorinsight.data.Group;
import org.jetbrains.research.refactorinsight.data.RefactoringInfo;
import org.jetbrains.research.refactorinsight.data.RefactoringLine;
import org.jetbrains.research.refactorinsight.data.types.Handler;
import org.refactoringminer.api.Refactoring;

public class ModifyClassAnnotationHandler extends Handler {

  @Override
  public RefactoringInfo specify(Refactoring refactoring, RefactoringInfo info) {
    ModifyClassAnnotationRefactoring ref = (ModifyClassAnnotationRefactoring) refactoring;

    if (ref.getClassAfter().isInterface()) {
      info.setGroup(Group.INTERFACE);
    } else if (ref.getClassAfter().isAbstract()) {
      info.setGroup(Group.ABSTRACT);
    } else {
      info.setGroup(Group.CLASS);
    }

    return info
        .setDetailsBefore(ref.getClassBefore().getPackageName())
        .setDetailsAfter(ref.getClassAfter().getPackageName())
        .setNameBefore(ref.getClassBefore().getName())
        .setNameAfter(ref.getClassAfter().getName())
        .setElementBefore(ref.getAnnotationBefore().toString())
        .setElementAfter(ref.getAnnotationAfter().toString())
        .addMarking(ref.getAnnotationBefore().codeRange(), ref.getAnnotationAfter().codeRange(),
            line -> line.addOffset(ref.getAnnotationBefore().getLocationInfo(),
                ref.getAnnotationAfter().getLocationInfo()),
            RefactoringLine.MarkingOption.NONE, true);
  }
}