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

  private static final FieldMask2<Message> KEEP_NONE = new FieldMask2<>(Collections.emptyMap(), Collections.emptySet(), false, true, null);
  private static final FieldMask2<Message> KEEP_ALL = new FieldMask2<>(Collections.emptyMap(), Collections.emptySet(), true, false, null);

  private final Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subMessageMasks;
  private final Set<Descriptors.FieldDescriptor> primitiveMasks;
  private final boolean keepAll;
  private final boolean keepNone;
  private final Descriptors.Descriptor messageDescriptor;

  private FieldMask2(Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subMessageMasks, Set<Descriptors.FieldDescriptor> primitiveMasks, boolean keepAll, boolean keepNone, Descriptors.Descriptor messageDescriptor) {
    this.subMessageMasks = subMessageMasks;
    this.primitiveMasks = primitiveMasks;
    this.keepAll = keepAll;
    this.keepNone = keepNone;
    this.messageDescriptor = messageDescriptor;
  }

  public T scrub(T message) {
    if (keepAll) {
      return message;
    }
    if (keepNone) {
      return (T) message.getDefaultInstanceForType();
    }
    checkCompatible(message.getDescriptorForType());

    Message.Builder builder = message.newBuilderForType();
    mergeInner(message, builder);
    return (T) builder.build();
  }

  public void merge(T message, Message.Builder builder) {
    if (messageDescriptor == null) {
      throw new IllegalArgumentException("Can't use merge with internal field masks");
    }
    checkCompatible(builder.getDescriptorForType());

    if (keepAll) {
      builder.mergeFrom(message);
      return;
    }
    if (keepNone) {
      return;
    }


    mergeInner(message, builder);
  }

  private void checkCompatible(Descriptors.Descriptor otherDescriptor) {
    if (messageDescriptor != otherDescriptor) {
      throw new IllegalArgumentException("Other descriptor is not compatible. Expected " + messageDescriptor.getFullName() + " but got " + otherDescriptor.getFullName());
    }
  }

  private void mergeInner(T message, Message.Builder builder) {
    for (Map.Entry<Descriptors.FieldDescriptor, FieldMask2<Message>> entry : subMessageMasks.entrySet()) {
      Descriptors.FieldDescriptor field = entry.getKey();
      FieldMask2<Message> subMask = entry.getValue();
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
    for (Descriptors.FieldDescriptor field : primitiveMasks) {
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

  public FieldMask2<T> union(FieldMask2<T> other) {
    if (keepAll || other.keepNone) {
      return this;
    }
    if (other.keepAll || keepNone) {
      return other;
    }

    checkCompatible(other.messageDescriptor);

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
    return new FieldMask2<>(subMessageMasks, primitiveMasks, false, false, messageDescriptor);
  }

  public FieldMask2<T> intersect(FieldMask2<T> other) {
    if (keepNone || other.keepAll) {
      return this;
    }
    if (other.keepNone || keepAll) {
      return other;
    }

    checkCompatible(other.messageDescriptor);

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

    return new FieldMask2<>(subMessageMasks, primitiveMasks, false, false, messageDescriptor);
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

  public static <T extends Message> FieldMask2<T> fromMessage(T message) {
    if (message.equals(message.getDefaultInstanceForType())) {
      return (FieldMask2<T>) KEEP_ALL;
    }

    Map<Descriptors.FieldDescriptor, FieldMask2<Message>> subs = new HashMap<>();
    HashSet<Descriptors.FieldDescriptor> primitiveMasks = new HashSet<>();

    final Map<Descriptors.FieldDescriptor, Object> fields = message.getAllFields();
    for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : fields.entrySet()) {
      Object value = entry.getValue();
      if (value != null) {
        Descriptors.FieldDescriptor key = entry.getKey();
        if (key.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
          Object value2 = getFirst(message, key);
          if (value2 != null) {
            subs.put(key, fromMessage((Message) value2));
          }
        } else {
          Object value2 = getFirst(message, key);
          if (value2 != null && !value2.equals(key.getDefaultValue())) {
            primitiveMasks.add(key);
          }
        }
      }
    }
    return new FieldMask2<>(subs, primitiveMasks, false, false, message.getDescriptorForType());
  }

  private static <T extends Message> Object getFirst(T message, Descriptors.FieldDescriptor key) {
    if (key.isRepeated()) {
      if (message.getRepeatedFieldCount(key) > 0) {
        return message.getRepeatedField(key, 0);
      }
      return null;
    }
    return message.getField(key);
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
      return new FieldMask2<>(Collections.singletonMap(field, child), Collections.emptySet(), false, false, descriptor);
    } else {
      if (index < segments.length - 1) {
        throw new PrimitiveFieldException(descriptor, segment);
      }

      return new FieldMask2<>(Collections.emptyMap(), Collections.singleton(field), false, false, descriptor);
    }
  }
}
