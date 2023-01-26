package org.klojang.jdbc.x.rs;

import org.klojang.check.Check;
import org.klojang.collections.TypeMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.jdbc.SQLTypeNames.getTypeName;
import static org.klojang.util.ClassMethods.className;

/**
 * Finds the most suitable of the {@code ResultSet.getXXX} methods for a given Java
 * type. If no sure-fire match can be found, then an {@link Adapter} function can be
 * specified that converts the result of the {@code getXXX} method to the required
 * Java type. Therefore what actually gets negotiated is not so much the
 * {@code getXXX} method per se, but an {@link RsExtractor}, which combines a
 * {@code getXXX} method with an (optional) converter function.
 */
@SuppressWarnings("rawtypes")
public class ExtractorNegotiator {

  private static ExtractorNegotiator INSTANCE;

  public static ExtractorNegotiator getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ExtractorNegotiator();
    }
    return INSTANCE;
  }

  private final Map<Class<?>, Map<Integer, RsExtractor>> all;

  private ExtractorNegotiator() {
    all = configure();
  }

  @SuppressWarnings({"unchecked"})
  public <T, U> RsExtractor<T, U> findExtractor(Class<U> fieldType, int sqlType) {
    // This implicitly checks that the specified int is one of the
    // static final int constants in the java.sql.Types class
    String sqlTypeName = getTypeName(sqlType);
    Map<Integer, RsExtractor> extractors = Check.that(all.get(fieldType))
        .is(notNull(), "Type not supported: %s", className(fieldType))
        .ok();
    return Check.that(extractors.get(sqlType))
        .is(notNull(), "Cannot convert %s to %s", sqlTypeName, className(fieldType))
        .ok();
  }

  private static Map configure() {
    return TypeMap.nativeTypeMapBuilder()
        .autobox(true)
        .add(String.class, my(new StringExtractors()))
        .add(Integer.class, my(new IntExtractors()))
        .add(Double.class, my(new DoubleExtractors()))
        .add(Long.class, my(new LongExtractors()))
        .add(Float.class, my(new FloatExtractors()))
        .add(Short.class, my(new ShortExtractors()))
        .add(Byte.class, my(new ByteExtractors()))
        .add(Boolean.class, my(new BooleanExtractors()))
        .add(LocalDate.class, my(new LocalDateExtractors()))
        .add(LocalDateTime.class, my(new LocalDateTimeExtractors()))
        .add(Enum.class, my(new EnumExtractors()))
        .freeze();
  }

  private static Map<Integer, RsExtractor> my(ExtractorLookup<?> src) {
    return Map.copyOf(src);
  }

}
