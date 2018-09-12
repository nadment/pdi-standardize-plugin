package org.pentaho.di.trans.steps.standardize;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

/**
 * This class encodes an RFC822-compliant email address is a simplified
 * container, hiding the gory details of the underlying Java Mail API.
 */
public class EmailAddress implements Cloneable, Comparable<EmailAddress>
{
    /*----------------------------------------------------------------------*\
                            Private Data Items
    \*----------------------------------------------------------------------*/

    /**
     * The address. The user doesn't get to see this.
     */
    private InternetAddress emailAddress = null;

    /*----------------------------------------------------------------------*\
                               Constructors
    \*----------------------------------------------------------------------*/

    /**
     * Constructs a new <tt>EmailMessage</tt> object from an
     * <tt>InternetAddress</tt> object. Only used internally.
     *
     * @param address   the address
     *
     * @throws EmailException  bad email address
     */
    EmailAddress (InternetAddress address)
        throws AddressException
    {
        this.emailAddress = address;
    }

    /**
     * Constructs a new <tt>EmailMessage</tt> object from an email address
     * string. The address string can be any RFC822-compliant email address.
     * Examples include:
     *
     * <blockquote>
     * <pre>
     * {@code
     * renoir@example.com
     * Pierre Auguste Renoir <renoir@example.com>
     * "Pierre Auguste Renoir" <renoir@example.com>
     * renoir@example.com (Pierre Auguste Renoir)
     * }
     * </pre>
     * </blockquote>
     *
     * The <i>@domain</i> portion must be present.
     *
     * @param address   the address
     *
     * @throws EmailException  bad email address
     */
    public EmailAddress (String address)
        throws AddressException
    {
       
            // The InternetAddress class in the Java Mail 1.3 API has a
            // constructor that does strict parsing. But the 1.1 version
            // doesn't. However, we can use the parse() method to do the
            // checking; it does do strict RFC822 syntax checks.

            InternetAddress[] addresses = InternetAddress.parse (address);

            if (addresses.length != 1)
            {
                throw new AddressException ("\"" +
                                          address +
                                          "\" is an improperly formed " +
                                          "email address");
            }

            this.emailAddress = addresses[0];
    }

    /**
     * Constructs a new copy of an existing <tt>EmailMessage</tt> object.
     *
     * @param emailAddress The <tt>EmailMessage</tt> object to copy
     *
     * @throws EmailException  bad email address
     */
    public EmailAddress (EmailAddress emailAddress)
        throws AddressException
    {
        this (emailAddress.getInternetAddress());
    }

    /*----------------------------------------------------------------------*\
                              Public Methods
    \*----------------------------------------------------------------------*/

    /**
     * Determine whether this email address is equivalent to another email
     * address, by comparing the normalized address strings.
     *
     * @param obj  the other email address. Must be an <tt>EmailAddress</tt>
     *             object.
     *
     * @return <tt>true</tt> if the addresses are equivalent, <tt>false</tt>
     *         otherwise
     */
    public boolean equals (Object obj)
    {
        EmailAddress other = (EmailAddress) obj;
        
        return (this.compareTo (other) == 0);
    }

    /**
     * Get the hash code for this object.
     *
     * @return the hash code
     */
    public int hashCode()
    {
        return getAddress().hashCode();
    }

    /**
     * Compare two email addresses.
     *
     * @param email  the other email address. Must be an <tt>EmailAddress</tt>
     *             object.
     *
     * @return A negative number if this email address is lexicographically
     *         less than <tt>obj</tt>; 0 if the two addresses are
     *         equivalent; a postive number if this email address is
     *         lexicographically greater than <tt>obj</tt>.
     */
    public int compareTo (EmailAddress email)
    {
        return this.getAddress().compareToIgnoreCase (email.getAddress());
    }

    /**
     * Get the RFC822-compliant email address string associated with this
     * <tt>EmailAddress</tt> object.
     *
     * @return the email address string. This method will never return null
     *
     * @see #getPersonalName()
     */
    public String getAddress()
    {
        return this.emailAddress.getAddress();
    }

    /**
     * Get the user friend personal name associated with this
     * <tt>EmailAddress</tt> object.
     *
     * @return the personal name, or null if not present
     *
     * @see #getAddress()
     */
    public String getPersonalName()
    {
        return this.emailAddress.getPersonal();
    }



    /**
     * Convert this object into an RFC822-compliant email address.
     *
     * @return the email address
     */
    public String toString()
    {
        return this.emailAddress.toString();
    }

    /**
     * Clone this object.
     *
     * @return the clone
     */
    public Object clone() throws CloneNotSupportedException
    {
        Object result = null;

        try
        {
            result = new EmailAddress (this);
        }
        catch (AddressException ex)
        {
            result = null;
        }

        return result;
    }

    /*----------------------------------------------------------------------*\
                          Package-visible Methods
    \*----------------------------------------------------------------------*/

    /**
     * Get the RFC822-compliant email address associated with this
     * <tt>EmailAddress</tt> object, as an <tt>InternetAddress</tt> object.
     *
     * @return the email address string. This method will never return null
     *
     * @see #getAddress()
     * @see #getPersonalName()
     */
    InternetAddress getInternetAddress()
    {
        return this.emailAddress;
    }
}