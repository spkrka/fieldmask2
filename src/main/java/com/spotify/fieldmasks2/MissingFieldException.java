package com.spotify.fieldmasks2;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

public class MissingFieldException extends FieldMaskException {
  public <T extends Message> MissingFieldException(Descriptors.Descriptor descriptor, String pathSegment) {
    super("No such field '" + pathSegment + "' in " + descriptor.getFullName());
  }
}
