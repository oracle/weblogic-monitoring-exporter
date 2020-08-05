// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

import static org.objectweb.asm.Opcodes.ASM8;

/**
 * A support class for unit testing Mojo implementations that can query the Maven annotations, even though they
 * are not available via reflection.
 */
public class MojoTestSupport {

  private static final int ASM_VERSION = ASM8;
  private static final String DOT = "\\.";

  private final Class<? extends AbstractMojo> mojoClass;
  private final Map<String, AnnotationInfo> classAnnotations = new HashMap<>();
  private final Map<Field, Map<String, AnnotationInfo>> fieldAnnotations = new HashMap<>();

  public MojoTestSupport(Class<? extends AbstractMojo> mojoClass) throws IOException {
    this.mojoClass = mojoClass;
    ClassReader classReader = new ClassReader(this.mojoClass.getName());
    classReader.accept(new Visitor(this.mojoClass), 0);
  }

  /**
   * Returns a map of the fields of the @Mojo annotation associated with the mojo class.
   * @return a map of mojo annotation field names to values, as defined by the annotation
   */
  public Map<String, Object> getClassAnnotation() {
    return classAnnotations.get(toDescription(Mojo.class)).fields;
  }

  private String toDescription(Class<? extends Annotation> aaClass) {
    return "L" + aaClass.getName().replaceAll(DOT, "/") + ';';
  }

  /**
   * Returns the field on the specified mojo whose name is specified.
   * @param fieldName the name of the field to return
   * @throws NoSuchFieldException if no such field exists
   */
  public Field getParameterField(String fieldName) throws NoSuchFieldException {
    return mojoClass.getDeclaredField(fieldName);
  }

  /**
   * Returns a map of the fields of the @Parameter annotation associated with a named mojo field.
   * @param fieldName the name of the field
   * @return a map of parameter annotation field names to values, as defined by the annotation
   * @throws NoSuchFieldException if no such field exists
   */
  public Map<String, Object> getParameterAnnotation(String fieldName) throws NoSuchFieldException {
    return getFieldAnnotation(mojoClass.getDeclaredField(fieldName), Parameter.class).fields;
  }

  @SuppressWarnings("SameParameterValue")
  private AnnotationInfo getFieldAnnotation(Field field, Class<? extends Annotation> annotation) {
    return fieldAnnotations.get(field).get(toDescription(annotation));
  }

  static class AnnotationInfo {
    Map<String, Object> fields = new HashMap<>();
  }

  private abstract class MojoAnnotationVisitor extends AnnotationVisitor {

    private final String annotationClassDesc;
    private final Map<String, AnnotationInfo> annotations;

    MojoAnnotationVisitor(Map<String, AnnotationInfo> annotations, String desc) {
      super(ASM_VERSION);
      this.annotations = annotations;
      annotationClassDesc = desc;
      annotations.put(desc, new AnnotationInfo());
    }

    @Override
    public void visit(String name, Object value) {
      getOrCreateAnnotationInfo(annotationClassDesc, annotations).fields.put(name, value);
    }

    @Override
    public void visitEnum(String name, String enumDesc, String value) {
      getOrCreateAnnotationInfo(annotationClassDesc, annotations)
            .fields
            .put(name, getEnumConstant(getEnumClass(enumDesc), value));
    }

    private Class<?> getEnumClass(String desc) {
      try {
        String className = desc.substring(1, desc.length() - 1).replaceAll("/", DOT);
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e.toString());
      }
    }

    private Object getEnumConstant(Class<?> enumClass, String value) {
      for (Object constant : enumClass.getEnumConstants()) {
        if (value.equalsIgnoreCase(constant.toString())) {
          return constant;
        }
      }
      throw new RuntimeException("No enum constant " + value + " in " + enumClass);
    }
  }

  private AnnotationInfo getOrCreateAnnotationInfo(
        String description, Map<String, AnnotationInfo> map) {
    AnnotationInfo info = map.get(description);
    return info != null ? info : createAnnotationInfo(map, description);
  }

  private AnnotationInfo createAnnotationInfo(Map<String, AnnotationInfo> map, String description) {
    AnnotationInfo info = new AnnotationInfo();
    map.put(description, info);
    return info;
  }

  private class FieldAnnotationVisitor extends MojoAnnotationVisitor {

    FieldAnnotationVisitor(Map<String, AnnotationInfo> annotationMap, String desc) {
      super(annotationMap, desc);
    }
  }

  private class ClassAnnotationVisitor extends MojoAnnotationVisitor {

    ClassAnnotationVisitor(String annotationDescriptor) {
      super(classAnnotations, annotationDescriptor);
    }
  }

  private class MojoFieldVisitor extends FieldVisitor {

    private final Map<String, AnnotationInfo> annotationMap;

    MojoFieldVisitor(Field field) {
      super(ASM_VERSION);
      this.annotationMap = getOrCreateAnnotationMap(field);
    }

    Map<String, AnnotationInfo> getOrCreateAnnotationMap(Field field) {
      Map<String, AnnotationInfo> map = fieldAnnotations.get(field);
      return map != null ? map : createAnnotationMap(field);
    }

    Map<String, AnnotationInfo> createAnnotationMap(Field field) {
      Map<String, AnnotationInfo> map = new HashMap<>();
      fieldAnnotations.put(field, map);
      return map;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return new FieldAnnotationVisitor(annotationMap, desc);
    }
  }

  private class Visitor extends ClassVisitor {

    private final Class<?> theClass;

    Visitor(Class<?> theClass) {
      super(ASM_VERSION);
      this.theClass = theClass;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
          int typeRef, TypePath typePath, String desc, boolean visible) {
      return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return new ClassAnnotationVisitor(desc);
    }

    @Override
    public FieldVisitor visitField(int flags, String fieldName, String desc, String s, Object v) {
      try {
        return new MojoFieldVisitor(getField(fieldName));
      } catch (NoSuchFieldException e) {
        return super.visitField(flags, fieldName, desc, s, v);
      }
    }

    private Field getField(String fieldName) throws NoSuchFieldException {
      return theClass.getDeclaredField(fieldName);
    }
  }
}
