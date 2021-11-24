package repository;
/**
 *
 * <p>Title: </p>
 *
 * <p>Description: Each repository will implement this interface in order to use Repository Manager</p>
 *
 * <p>Copyright: Copyright (c) 2010. All Rights Reserved</p>
 *
 * <p>Company: Reve Systems Ltd </p>
 *
 * @author Ajmat Iqbal (Sajal)
 * @version 1.0
 */
public interface Repository
{
    /**
     * Reloads only updated row. It must not throw any exception.
     */
    public void reload(boolean realoadAll);

    /**
     * Returns the table name whose data it is reloading. Must be exact with vbSequencer table_name
     * @return String
     */
    public String getTableName();
    /**
     * It gets called when the system going to shutdown. Cleanup memory and thread in this method. Do not throw exception
     */
    public void shutDown();
}
