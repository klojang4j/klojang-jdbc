package org.klojang.jdbc.x.ps;

import org.klojang.jdbc.BindInfo;
import org.klojang.jdbc.NamedParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MapBinder {

  private static final Logger LOG = LoggerFactory.getLogger(MapBinder.class);

  private static final Object ABSENT = new Object();

  private List<NamedParameter> params;
  private BindInfo bindInfo;

  public MapBinder(List<NamedParameter> params, BindInfo bindInfo) {
    this.params = params;
    this.bindInfo = bindInfo;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void bindMap(
      PreparedStatement ps, Map<String, Object> map, Collection<NamedParameter> bound)
      throws Throwable {
    ReceiverNegotiator negotiator = ReceiverNegotiator.getInstance();
    for (NamedParameter param : params) {
      String key = param.getName();
      if (!map.containsKey(key)) {
        continue;
      }
      bound.add(param);
      Object input = map.getOrDefault(key, ABSENT);
      if (input == ABSENT) {
        continue;
      } else if (input == null) {
        param.getIndices().forEachThrowing(i -> ps.setString(i, null));
      } else {
        Receiver receiver;
        if (input instanceof Enum && bindInfo.bindEnumUsingToString(key)) {
          receiver = EnumReceivers.ENUM_TO_STRING;
        } else {
          receiver = negotiator.getDefaultReceiver(input.getClass());
        }
        Object output = receiver.getParamValue(input);
        if (LOG.isDebugEnabled()) {
          if (input == output) {
            LOG.debug("-> Parameter \"{}\": {}", key, output);
          } else {
            LOG.debug("-> Parameter \"{}\": {} (map value: {})", key, output, input);
          }
        }
        param.getIndices().forEachThrowing(i -> receiver.bind(ps, i, output));
      }
    }
  }
}
