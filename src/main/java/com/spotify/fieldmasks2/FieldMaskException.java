package com.spotify.fieldmasks2;

public abstract class FieldMaskException extends RuntimeException {
  public FieldMaskException(String message) {
    super(message);
  }
}
