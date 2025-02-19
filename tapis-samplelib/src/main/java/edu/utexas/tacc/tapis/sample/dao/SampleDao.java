package edu.utexas.tacc.tapis.sample.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.sample.dao.sql.SqlStatements;
import edu.utexas.tacc.tapis.sample.model.Sample;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJDBCException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public class SampleDao
 extends AbstractDao
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = LoggerFactory.getLogger(SampleDao.class);
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createSample:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Insert a new sample text record.  
     * 
     * @param text the input string
     * @throws TapisException on error
     */
    public void createSample(String text) 
      throws TapisException
    {
        // ------------------------- Check Input -------------------------
        // Exceptions can be throw from here.
        if (StringUtils.isBlank(text)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "createSample", "text");
            _log.error(msg);
            throw new TapisException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        Connection conn = null;
        try
        {
            // Get a database connection.
            conn = getConnection();

            // Set the sql command.
            String sql = SqlStatements.CREATE_SAMPLE;

            // Prepare the statement and fill in the placeholders.
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, text);

            // Issue the call.
            int rows = pstmt.executeUpdate();
            if (rows != 1) {
                String msg = MsgUtils.getMsg("DB_UPDATE_UNEXPECTED_ROWS", 1, rows, sql, text);
                _log.error(msg);
                throw new TapisException(msg);
            }

            // Commit the transaction.
            conn.commit();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {if (conn != null) conn.rollback();}
            catch (Exception e1){_log.error(MsgUtils.getMsg("DB_FAILED_ROLLBACK"), e1);}
            
            // Log the exception.
            String msg = MsgUtils.getMsg("DB_INSERT_FAILURE", "sample_tbl");
            _log.error(msg, e);
            throw new TapisException(msg, e);
        }
        finally {
            // Conditionally return the connection back to the connection pool.
            if (conn != null)
                try {conn.close();}
                catch (Exception e)
                {
                    // If commit worked, we can swallow the exception.
                    // If not, the commit exception will be thrown.
                    String msg = MsgUtils.getMsg("DB_FAILED_CONNECTION_CLOSE");
                    _log.error(msg, e);
                }
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* getSampleById:                                                         */
    /* ---------------------------------------------------------------------- */
    public Sample getSampleById(int id) 
     throws TapisException
    {
        // Initialize result.
        Sample result = null;

        // ------------------------- Call SQL ----------------------------
        Connection conn = null;
        try
        {
            // Get a database connection.
            conn = getConnection();
            
            // Get the select command.
            String sql = SqlStatements.SELECT_SAMPLE_BY_ID;
            
            // Prepare the statement and fill in the placeholders.
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
                        
            // Issue the call for the 1 row result set.
            ResultSet rs = pstmt.executeQuery();
            result = populateSample(rs);
            
            // Close the result and statement.
            rs.close();
            pstmt.close();
      
            // Commit the transaction.
            conn.commit();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {if (conn != null) conn.rollback();}
                catch (Exception e1){_log.error(MsgUtils.getMsg("DB_FAILED_ROLLBACK"), e1);}
            
            String msg = MsgUtils.getMsg("DB_SELECT_ID_ERROR", "Sample", id, e.getMessage());
            _log.error(msg, e);
            throw new TapisException(msg, e);
        }
        finally {
            // Always return the connection back to the connection pool.
            try {if (conn != null) conn.close();}
              catch (Exception e) 
              {
                // If commit worked, we can swallow the exception.  
                // If not, the commit exception will be thrown.
                String msg = MsgUtils.getMsg("DB_FAILED_CONNECTION_CLOSE");
                _log.error(msg, e);
              }
        }
        
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getSamples:                                                            */
    /* ---------------------------------------------------------------------- */
    public List<Sample> getSamples() 
     throws TapisException
    {
        // The result list is always non-null.
        var list = new ArrayList<Sample>();
        
        // ------------------------- Call SQL ----------------------------
        Connection conn = null;
        try
        {
            // Get a database connection.
            conn = getConnection();
            
            // Get the select command.
            String sql = SqlStatements.SELECT_ALL_SAMPLES;
            
            // Prepare the statement and fill in the placeholders.
            PreparedStatement pstmt = conn.prepareStatement(sql);
                        
            // Issue the call for the 1 row result set.
            ResultSet rs = pstmt.executeQuery();
            Sample sample = populateSample(rs);
            while (sample != null) {
                list.add(sample);
                sample = populateSample(rs);
            }
            
            // Close the result and statement.
            rs.close();
            pstmt.close();
      
            // Commit the transaction.
            conn.commit();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {if (conn != null) conn.rollback();}
                catch (Exception e1){_log.error(MsgUtils.getMsg("DB_FAILED_ROLLBACK"), e1);}
            
            String msg = MsgUtils.getMsg("DB_QUERY_ERROR", "samples", e.getMessage());
            _log.error(msg, e);
            throw new TapisException(msg, e);
        }
        finally {
            // Always return the connection back to the connection pool.
            try {if (conn != null) conn.close();}
              catch (Exception e) 
              {
                // If commit worked, we can swallow the exception.  
                // If not, the commit exception will be thrown.
                String msg = MsgUtils.getMsg("DB_FAILED_CONNECTION_CLOSE");
                _log.error(msg, e);
              }
        }
        
        return list;
    }
    
    /* ********************************************************************** */
    /*                             Private Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* populateSample:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Instantiate and populate a sample object with the results from a single
     * row from the sample_tbl table.
     * 
     * @param rs the result set for one job
     * @return the new, fully populated job object or null if the result set is empty 
     * @throws TapisJDBCException 
     */
    private Sample populateSample(ResultSet rs) 
     throws TapisJDBCException
    {
        // Quick check.
        if (rs == null) return null;
        
        // Return null if the results are empty or exhausted.
        // This call advances the cursor.
        try {if (!rs.next()) return null;}
        catch (Exception e) {
          String msg = MsgUtils.getMsg("DB_RESULT_ACCESS_ERROR", e.getMessage());
          _log.error(msg, e);
          throw new TapisJDBCException(msg, e);
        }
        
        // Create the mostly empty job and then overwrite all fields.
        Sample sample = new Sample();
        try {
            sample.setId(rs.getInt(1));
            sample.setText(rs.getString(2));
            sample.setUpdated(rs.getTimestamp(3).toInstant());
        } 
          catch (Exception e) {
            String msg = MsgUtils.getMsg("DB_TYPE_CAST_ERROR", e.getMessage());
            _log.error(msg, e);
            throw new TapisJDBCException(msg, e);
          }
        
        return sample;
    }
    
}
