package com.spotify.fieldmasks2;

import com.google.protobuf.Descriptors;

public class PrimitiveFieldException extends FieldMaskException {
  public PrimitiveFieldException(Descriptors.Descriptor descriptor, String segment) {
    super("Can't follow path for primitive field '" + segment + "' in " + descriptor.getFullName());
  }
}
