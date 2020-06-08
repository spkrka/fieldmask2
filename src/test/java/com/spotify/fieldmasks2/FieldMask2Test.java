package com.spotify.fieldmasks2;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.util.FieldMaskUtil;
import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class FieldMask2Test {

  @Test
  public void testEmpty() {
    DescriptorProtos.DescriptorProto input = DescriptorProtos.DescriptorProto.newBuilder()
            .setName("the name")
            .setOptions(DescriptorProtos.MessageOptions.newBuilder()
                    .setDeprecated(true)
                    .setMapEntry(false)
                    .build())
            .build();

    DescriptorProtos.DescriptorProto expected = DescriptorProtos.DescriptorProto.getDefaultInstance();

    FieldMask2<DescriptorProtos.DescriptorProto> mask2 = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "");
    assertEquals(expected, mask2.scrub(input));
  }

  @Test
  public void testSimple() {
    DescriptorProtos.DescriptorProto input = DescriptorProtos.DescriptorProto.newBuilder()
            .setName("the name")
            .setOptions(DescriptorProtos.MessageOptions.newBuilder()
                    .setDeprecated(true)
                    .setMapEntry(false)
                    .build())
            .build();

    DescriptorProtos.DescriptorProto expected = DescriptorProtos.DescriptorProto.newBuilder()
            .setName("the name")
            .setOptions(DescriptorProtos.MessageOptions.newBuilder()
                    .setDeprecated(true)
                    .build())
            .build();

    FieldMask2<DescriptorProtos.DescriptorProto> mask2 = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.deprecated");

    DescriptorProtos.DescriptorProto actual = mask2.scrub(input);
    assertEquals(expected, actual);
  }

  @Test
  public void testUnion1() {
    FieldMask2<DescriptorProtos.DescriptorProto> first = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.deprecated");
    FieldMask2<DescriptorProtos.DescriptorProto> second = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options");

    FieldMask2<DescriptorProtos.DescriptorProto> expected = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options");

    FieldMask2<DescriptorProtos.DescriptorProto> actual = first.union(second);

    assertEquals(expected, actual);

  }

  @Test
  public void testUnion2() {
    FieldMask2<DescriptorProtos.DescriptorProto> first = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.deprecated");
    FieldMask2<DescriptorProtos.DescriptorProto> second = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.map_entry");

    FieldMask2<DescriptorProtos.DescriptorProto> expected = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.deprecated,options.map_entry");

    FieldMask2<DescriptorProtos.DescriptorProto> actual = first.union(second);

    assertEquals(expected, actual);
  }

  @Test
  public void testIntersection() {
    FieldMask2<DescriptorProtos.DescriptorProto> first = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.deprecated");
    FieldMask2<DescriptorProtos.DescriptorProto> second = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name,options.map_entry");

    DescriptorProtos.DescriptorProto input = DescriptorProtos.DescriptorProto.newBuilder()
            .setName("the name")
            .setOptions(DescriptorProtos.MessageOptions.newBuilder()
                    .setDeprecated(true)
                    .setMapEntry(false)
                    .build())
            .build();

    DescriptorProtos.DescriptorProto expected = DescriptorProtos.DescriptorProto.newBuilder()
            .setName("the name")
            .setOptions(DescriptorProtos.MessageOptions.newBuilder()
                    .build())
            .build();

    assertEquals(expected, first.intersect(second).scrub(input));
  }

  @Test
  public void testRepeated() {

    DescriptorProtos.DescriptorProto input = DescriptorProtos.DescriptorProto.newBuilder()
            .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder()
                    .addReservedName("reserved 1")
                    .setName("the name 1")
                    .build())
            .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder()
                    .addReservedName("reserved 2")
                    .setName("the name 2")
                    .build())
            .build();

    DescriptorProtos.DescriptorProto expected = DescriptorProtos.DescriptorProto.newBuilder()
            .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder()
                    .addReservedName("reserved 1")
                    .build())
            .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder()
                    .addReservedName("reserved 2")
                    .build())
            .build();

    FieldMask2<DescriptorProtos.DescriptorProto> mask = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "enum_type.reserved_name");
    assertEquals(expected, mask.scrub(input));
  }

  @Test(expected = MissingFieldException.class)
  public void testMissingField() {
    FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name2");
  }

  @Test(expected = PrimitiveFieldException.class)
  public void testNotASubMessage() {
    FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), "name.options");
  }

  @Test
  public void testFromFieldMask() {
    String input = "enum_type.reserved_name,enum_type.name,options.deprecated,extension";
    FieldMask2<DescriptorProtos.DescriptorProto> expected = FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), input);
    FieldMask2<DescriptorProtos.DescriptorProto> actual = FieldMask2.fromFieldMask(DescriptorProtos.DescriptorProto.getDefaultInstance(), FieldMaskUtil.fromString(input));
    assertEquals(actual, expected);
  }

  @Test
  public void testToFieldMask() {
    String input = "enum_type.reserved_name,enum_type.name,options.deprecated,extension";
    Set<String> expected = new TreeSet<>(FieldMaskUtil.fromString(input).getPathsList());
    Set<String> actual = new TreeSet<>(FieldMask2.create(DescriptorProtos.DescriptorProto.getDefaultInstance(), input).toFieldMask().getPathsList());
    assertEquals(actual, expected);
  }
}