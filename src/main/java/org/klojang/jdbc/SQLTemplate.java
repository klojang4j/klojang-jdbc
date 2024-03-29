package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.jdbc.x.Utils;
import org.klojang.jdbc.x.sql.ParamExtractor;
import org.klojang.templates.ParseException;
import org.klojang.templates.Template;

import java.sql.Connection;

import static org.klojang.jdbc.x.Strings.CONNECTION;

final class SQLTemplate extends AbstractSQL {

  private final Template template;
  private final ParamExtractor extractor;

  SQLTemplate(String sql, SessionConfig config) {
    super(sql, config);
    extractor = new ParamExtractor(sql);
    try {
      template = Template.fromString(extractor.getNormalizedSQL());
    } catch (ParseException e) {
      throw Utils.wrap(e);
    }
  }

  @Override
  public SQLSession session(Connection con) {
    Check.notNull(con, CONNECTION);
    return new SQLTemplateSession(con, this, extractor, template.newRenderSession());
  }

}
