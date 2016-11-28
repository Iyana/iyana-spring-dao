/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.iyana.springdao.exception;

import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import uk.co.iyana.commons.usererror.ErrorContext;
import uk.co.iyana.commons.usererror.ContextNameTranslator;
import uk.co.iyana.commons.usererror.StandardErrorResourceNames;
import uk.co.iyana.commons.usererror.UserErrorException;
import uk.co.iyana.commons.usererror.UserErrorMessageType;
import uk.co.iyana.commons.validation.StandardValidationErrorResourceNames;
import uk.co.iyana.springdao.ConstraintType;
import uk.co.iyana.springdao.DatabaseProduct;

/**
 *
 * @author fgyara
 */
public class StructuredDataIntegrityViolationException extends DataIntegrityViolationException {

    private String violatedConstraintName;
    private ConstraintType violatedConstraintType;
    
    public StructuredDataIntegrityViolationException(DatabaseProduct product, String task, String sql, SQLIntegrityConstraintViolationException sqlEx) {
        super( "Constraint violated: " + sqlEx.getMessage(), sqlEx);
        
        // map
        switch (product) {
            case ORACLE:
                processExceptionForOracle(sqlEx);
                break;
            default:
                violatedConstraintName = "Not known";
                violatedConstraintType = ConstraintType.NOT_KNOWN;
        }
    }
    
    
    /**
     * @return the violatedConstraintName
     */
    public final String getViolatedConstraintName() {
        return violatedConstraintName;
    }

    /**
     * @return the violatedConstraintType
     */
    public ConstraintType getViolatedConstraintType() {
        return violatedConstraintType;
    }

    private void processExceptionForOracle(SQLIntegrityConstraintViolationException e) {
        String sqlErrorMessage = e.getMessage();
        
        
        switch (e.getErrorCode()) {
            case 1:
                // ORA-00001: unique constraint (GENZR1.LOOKUP_LOOKUP_VALUE_KEY) violated
                this.violatedConstraintType = ConstraintType.UNIQUE_KEY;
                String fullConstraintName =  sqlErrorMessage.substring(sqlErrorMessage.indexOf("(") + 1, sqlErrorMessage.indexOf(")"));
                this.violatedConstraintName = fullConstraintName.substring(fullConstraintName.indexOf(".") + 1);
                break;
                
            case 1400:
                // ORA-01400: cannot insert NULL into ("GENZR1"."G_LOOKUP_T"."GRP")
                this.violatedConstraintType = ConstraintType.NOT_NULL;
                fullConstraintName =  sqlErrorMessage.substring(sqlErrorMessage.indexOf("(") + 1, sqlErrorMessage.indexOf(")"));
                String[] constraintNames = fullConstraintName.split("\\.");
                this.violatedConstraintName = constraintNames[2].substring(1, constraintNames[2].length()-1);
                break;
                
            case 2291:
                // ORA-02291: integrity constraint (GENZR1.ADDRESS_COUNTRY) violated - parent key not found
                this.violatedConstraintType = ConstraintType.PARENT_KEY_NOT_FOUND;
                fullConstraintName =  sqlErrorMessage.substring(sqlErrorMessage.indexOf("(") + 1, sqlErrorMessage.indexOf(")"));
                this.violatedConstraintName = fullConstraintName.substring(fullConstraintName.indexOf(".") + 1);
                break;
                
            default:
                this.violatedConstraintType = ConstraintType.NOT_KNOWN;
                this.violatedConstraintName = "Not known: " + e.getErrorCode() + " - " + e.getMessage();
                System.out.println("Exception: " + this.violatedConstraintName);
        }
        
//        System.out.println("Violated Constraint name: " + violatedConstraintName);
//        System.out.println("SQL State:" + e.getErrorCode());
//        System.out.println("Message:" + e.getMessage());
//        System.out.println();
    }

    public UserErrorException toUserErrorException(ErrorContext errorContext, ContextNameTranslator contextNameTranslator) {
        // convert the constraint type
        String resourceName;
        Object[] params = null;
        
        switch (this.violatedConstraintType) {
            case NOT_NULL:
                resourceName = StandardValidationErrorResourceNames.NULL_NOT_ALLOWED;
                errorContext = new ErrorContext( errorContext, contextNameTranslator.xlatColumnNameToFieldName(this.violatedConstraintName));
                break;
                
            case PARENT_KEY_NOT_FOUND:
                resourceName = StandardErrorResourceNames.FOREIGN_KEY_VIOLATION;
                errorContext = new ErrorContext( errorContext, contextNameTranslator.xlatParentKeyNameToFieldName(this.violatedConstraintName));
                break;
                
            case UNIQUE_KEY:
                resourceName = StandardErrorResourceNames.UNIQUE_KEY_VIOLATION;
                errorContext = new ErrorContext( errorContext, contextNameTranslator.xlatUniqueKeyNameToFieldName(this.violatedConstraintName));
                params = contextNameTranslator.xlatUniqueKeyNameToFieldNames(this.violatedConstraintName);
                break;
                
            case NOT_KNOWN:
            default:
                resourceName = StandardErrorResourceNames.UNCAUGHT_THROWABLE;
        }
        
        return new UserErrorException(
            errorContext,
            UserErrorMessageType.RESOURCE, 
            resourceName, 
            null,
            params);
    }

}
