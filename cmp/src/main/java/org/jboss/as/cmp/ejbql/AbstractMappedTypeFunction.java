package org.jboss.as.cmp.ejbql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import org.jboss.as.cmp.jdbc.JDBCResultSetReader;
import org.jboss.as.cmp.jdbc.JDBCType;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.logging.Logger;

/**
 * A AbstractMappedTypeFunction.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractMappedTypeFunction extends SimpleNode implements SelectFunction {
    protected final Logger log;
    protected Class<?> resultType;
    protected JDBCResultSetReader resultReader;

    public AbstractMappedTypeFunction(int i) {
        super(i);
        log = Logger.getLogger(getClass());
    }

    public void setResultType(Class<?> type) {
        if (Collection.class.isAssignableFrom(type)) {
            resultType = getDefaultResultType();
        } else {
            this.resultType = type;
        }
        this.resultReader = JDBCUtil.getResultReaderByType(resultType);
    }

    protected Class<?> getDefaultResultType() {
        return Double.class;
    }

    public void setJDBCType(final JDBCType jdbcType) {
        if (resultReader != null) {
            final JDBCResultSetReader jdbcResultReader = this.resultReader;
            resultReader = new JDBCResultSetReader() {
                public Object get(ResultSet rs, int index, Class destination, Logger log) throws SQLException {
                    Object jdbcResult = jdbcResultReader.get(rs, index, destination, log);
                    return jdbcType.setColumnValue(0, null, jdbcResult);
                }
            };
        }
    }

    // SelectFunction implementation

    public Object readResult(ResultSet rs) throws SQLException {
        return resultReader.get(rs, 1, resultType, log);
    }
}
