package uk.co.iyana.springdao.ect;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import uk.co.iyana.springdao.DatabaseProduct;
import uk.co.iyana.springdao.exception.StructuredDataIntegrityViolationException;

public class IyanaSQLErrorCodesTranslator extends SQLErrorCodeSQLExceptionTranslator {
    private final DatabaseProduct product;
    
    public IyanaSQLErrorCodesTranslator (DatabaseProduct product) {
        this.product = product;
    }
    
    
    @Override
    protected DataAccessException customTranslate(String task, String sql, SQLException sqlEx) {

        if (sqlEx instanceof SQLIntegrityConstraintViolationException) {
            return new StructuredDataIntegrityViolationException(product, task, sql, (SQLIntegrityConstraintViolationException)sqlEx);
        }

        return null;
    }
}
