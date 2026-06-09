package com.finflow.user.domain;

import java.security.SecureRandom;

final class PublicIds {
  private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
  private static final SecureRandom RANDOM = new SecureRandom();

  private PublicIds() {
  }

  static String next(String prefix) {
    var out = new char[18];
    for (int i = 0; i < out.length; i++) {
      out[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
    }
    return prefix + "_" + new String(out);
  }
}
