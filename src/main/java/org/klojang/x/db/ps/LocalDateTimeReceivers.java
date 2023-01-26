package org.klojang.x.db.ps;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import static java.sql.Types.*;
import static org.klojang.util.ObjectMethods.ifNotNull;
import static org.klojang.x.db.ps.PsSetter.SET_TIMESTAMP;

public class LocalDateTimeReceivers extends ReceiverLookup<LocalDateTime> {

  static final Receiver<LocalDateTime, Timestamp> DEFAULT =
      new Receiver<>(SET_TIMESTAMP, d -> ifNotNull(d, Timestamp::valueOf));

  LocalDateTimeReceivers() {
    put(TIMESTAMP, DEFAULT);
  }

  @Override
  Receiver<LocalDateTime, ?> getDefaultReceiver() {
    return DEFAULT;
  }
}
