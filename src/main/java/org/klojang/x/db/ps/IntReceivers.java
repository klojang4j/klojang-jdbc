package org.klojang.x.db.ps;

import org.klojang.convert.Bool;
import org.klojang.convert.NumberMethods;

import static java.sql.Types.*;
import static org.klojang.x.db.ps.PsSetter.SET_BOOLEAN;
import static org.klojang.x.db.ps.PsSetter.SET_BYTE;
import static org.klojang.x.db.ps.PsSetter.SET_INT;
import static org.klojang.x.db.ps.PsSetter.SET_SHORT;


class IntReceivers extends ReceiverLookup<Integer> {

  static final Receiver<Integer, Integer> DEFAULT = new Receiver<>(SET_INT);

  IntReceivers() {
    put(INTEGER, DEFAULT);
    put(SMALLINT, new Receiver<Integer, Short>(SET_SHORT, NumberMethods::convert));
    put(TINYINT, new Receiver<Integer, Byte>(SET_BYTE, NumberMethods::convert));
    putMultiple(new Receiver<Integer, Boolean>(SET_BOOLEAN, Bool::from), BOOLEAN, BIT);
    put(VARCHAR, Receiver.ANY_TO_STRING);
    put(CHAR, Receiver.ANY_TO_STRING);
  }

  @Override
  Receiver<Integer, ?> getDefaultReceiver() {
    return DEFAULT;
  }
}
