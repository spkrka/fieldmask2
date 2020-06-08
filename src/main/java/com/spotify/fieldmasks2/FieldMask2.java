package com.spotify.fieldmasks2;

import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FieldMask2<T extends Message> {

  private static final FieldMask2<Message> KEEP_NONE = new FieldMask2<>(Collections.emptyMap(), Collections.emptySet(), false, true);
  private static final FieldMask2<Message> KEEP_ALL = new FieldMask2<>(Collections.emptyMap(), Collections.emptySet(), true, false);

  private final Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subMessageMasks;
  private final Set<Descriptors.FieldDescriptor> primitiveMasks;
  private final boolean keepAll;
  private final boolean keepNone;

  public FieldMask2(Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subMessageMasks, Set<Descriptors.FieldDescriptor> primitiveMasks, boolean keepAll, boolean keepNone) {
    this.subMessageMasks = subMessageMasks;
    this.primitiveMasks = primitiveMasks;
    this.keepAll = keepAll;
    this.keepNone = keepNone;
  }

  public T scrub(T message) {
    if (keepAll) {
      return message;
    }
    if (keepNone) {
      return (T) message.getDefaultInstanceForType();
    }

    Message.Builder builder = message.newBuilderForType();

    Descriptors.Descriptor descriptorForType = message.getDescriptorForType();
    List<Descriptors.FieldDescriptor> fields = descriptorForType.getFields();
    for (Descriptors.FieldDescriptor field : fields) {
      Descriptors.FieldDescriptor.Type type = field.getType();
      if (type == Descriptors.FieldDescriptor.Type.MESSAGE) {
        FieldMask2<Message> subMask = subMessageMasks.get(field);
        if (subMask != null) {
          if (field.isRepeated()) {
            int count = message.getRepeatedFieldCount(field);
            for (int i = 0; i < count; i++) {
              Message value = (Message) message.getRepeatedField(field, i);
              builder.addRepeatedField(field, subMask.scrub(value));
            }
          } else {
            Message value = (Message) message.getField(field);
            if (value != null) {
              builder.setField(field, subMask.scrub(value));
            }
          }
        }
      } else {
        if (primitiveMasks.contains(field)) {
          if (field.isRepeated()) {
            int count = message.getRepeatedFieldCount(field);
            for (int i = 0; i < count; i++) {
              Object value = message.getRepeatedField(field, i);
              builder.addRepeatedField(field, value);
            }
          } else {
            Object value = message.getField(field);
            if (value != null) {
              builder.setField(field, value);
            }
          }
        }
      }

    }

    return (T) builder.build();
  }

  public FieldMask2<T> union(FieldMask2<T> other) {
    if (keepAll) {
      return this;
    }
    if (other.keepAll) {
      return other;
    }

    if (keepNone && other.keepNone) {
      return this;
    }

    Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subMessageMasks = new HashMap<>();
    HashSet<Descriptors.FieldDescriptor> primitiveMasks = new HashSet<>();

    subMessageMasks.putAll(this.subMessageMasks);
    for (Map.Entry<Descriptors.FieldDescriptor, FieldMask2<Message>> entry : other.subMessageMasks.entrySet()) {
      FieldMask2<Message> existingValue = subMessageMasks.get(entry.getKey());
      if (existingValue == null) {
        subMessageMasks.put(entry.getKey(), entry.getValue());
      } else {
        subMessageMasks.put(entry.getKey(), existingValue.union(entry.getValue()));
      }
    }
    primitiveMasks.addAll(this.primitiveMasks);
    primitiveMasks.addAll(other.primitiveMasks);
    return new FieldMask2<>(subMessageMasks, primitiveMasks, false, false);
  }

  public FieldMask2<T> intersect(FieldMask2<T> other) {
    if (keepNone) {
      return this;
    }
    if (other.keepNone) {
      return other;
    }

    if (keepAll && other.keepAll) {
      return this;
    }

    Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subMessageMasks = new HashMap<>();
    HashSet<Descriptors.FieldDescriptor> primitiveMasks = new HashSet<>();

    subMessageMasks.putAll(this.subMessageMasks);
    for (Map.Entry<Descriptors.FieldDescriptor, FieldMask2<Message>> entry : other.subMessageMasks.entrySet()) {
      FieldMask2<Message> existingValue = subMessageMasks.get(entry.getKey());
      if (existingValue == null) {
        subMessageMasks.remove(entry.getKey());
      } else {
        subMessageMasks.put(entry.getKey(), existingValue.intersect(entry.getValue()));
      }
    }
    primitiveMasks.addAll(this.primitiveMasks);
    primitiveMasks.retainAll(other.primitiveMasks);

    return new FieldMask2<>(subMessageMasks, primitiveMasks, false, false);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FieldMask2<?> that = (FieldMask2<?>) o;
    return keepAll == that.keepAll &&
            keepNone == that.keepNone &&
            subMessageMasks.equals(that.subMessageMasks) &&
            primitiveMasks.equals(that.primitiveMasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subMessageMasks, primitiveMasks, keepAll, keepNone);
  }

  public static <T extends Message> FieldMask2<T> create(T template, String... masks) {
    return create(template, Arrays.asList(masks));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb, 0);
    return sb.toString();
  }

  private void toString(StringBuilder sb, int indent) {
    if (keepAll) {
      indent(sb, indent);
      sb.append("*\n");
      return;
    }

    if (keepNone) {
      return;
    }

    for (Descriptors.FieldDescriptor primitiveMask : primitiveMasks) {
      indent(sb, indent);
      sb.append(primitiveMask.getName()).append("\n");
    }

    for (Map.Entry<Descriptors.FieldDescriptor, FieldMask2<Message>> entry : subMessageMasks.entrySet()) {
      indent(sb, indent);
      sb.append(entry.getKey().getName()).append(":\n");
      entry.getValue().toString(sb, indent + 2);
    }
  }

  private void indent(StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append(' ');
    }
  }

  public FieldMask toFieldMask() {
    FieldMask.Builder builder = FieldMask.newBuilder();
    addPaths(builder, new StringBuilder());
    return builder.build();
  }

  private void addPaths(FieldMask.Builder builder, StringBuilder sb) {
    for (Descriptors.FieldDescriptor primitiveMask : primitiveMasks) {
      int prevLen = sb.length();
      if (prevLen > 0) {
        sb.append('.');
      }
      sb.append(primitiveMask.getName());
      builder.addPaths(sb.toString());
      sb.setLength(prevLen);
    }
    for (Map.Entry<Descriptors.FieldDescriptor, FieldMask2<Message>> entry : subMessageMasks.entrySet()) {
      int prevLen = sb.length();
      if (prevLen > 0) {
        sb.append('.');
      }
      String name = entry.getKey().getName();
      sb.append(name);
      FieldMask2<Message> child = entry.getValue();
      if (child.keepAll) {
        builder.addPaths(sb.toString());
      } else {
        child.addPaths(builder, sb);
      }
      sb.setLength(prevLen);
    }
  }

  public static <T extends Message> FieldMask2<T> fromFieldMask(T template, FieldMask fieldMask) {
    return create(template, fieldMask.getPathsList());
  }

  public static <T extends Message> FieldMask2<T> create(T template, List<String> masks) {
    FieldMask2<Message> current = KEEP_NONE;
    for (String mask : masks) {
      current = current.union(createSingleLine(template.getDescriptorForType(), mask));
    }
    return (FieldMask2<T>) current;
  }

  private static FieldMask2<Message> createSingleLine(Descriptors.Descriptor descriptor, String maskLine) {
    if (maskLine.contains(",")) {
      FieldMask2<Message> current = KEEP_NONE;
      String[] split = maskLine.split(",");
      for (String s : split) {
        if (!s.isEmpty()) {
          current = current.union(createSinglePath(descriptor, s));
        }
      }
      return current;
    } else if (maskLine.isEmpty()) {
      return KEEP_NONE;
    } else {
      return createSinglePath(descriptor, maskLine);
    }
  }

  private static FieldMask2<Message> createSinglePath(Descriptors.Descriptor descriptor, String maskPath) {
    String[] split = maskPath.split("\\.");
    return createSinglePath(descriptor, split, 0);
  }

  private static FieldMask2<Message> createSinglePath(Descriptors.Descriptor descriptor, String[] segments, int index) {
    if (index == segments.length) {
      return KEEP_ALL;
    }

    String segment = segments[index];

    Descriptors.FieldDescriptor field = descriptor.findFieldByName(segment);

    if (field == null) {
      throw new MissingFieldException(descriptor, segment);
    }

    if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
      Descriptors.Descriptor childDescriptor = field.getMessageType();
      FieldMask2<Message> child = createSinglePath(childDescriptor, segments, index + 1);
      return new FieldMask2<>(Collections.singletonMap(field, child), Collections.emptySet(), false, false);
    } else {
      if (index < segments.length - 1) {
        throw new PrimitiveFieldException(descriptor, segment);
      }

      return new FieldMask2<>(Collections.emptyMap(), Collections.singleton(field), false, false);
    }
  }
}
