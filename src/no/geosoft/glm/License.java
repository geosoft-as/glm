package no.geosoft.glm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Model a license for a specific application on a set of hardware
 * instances within a particular time period.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class License
{
  /** The license holder, i.e. the licensee. Non-null. */
  private final String licensee_;

  /** License issuer, such as GeoSoft AS. Non-null. */
  private final String issuer_;

  /**
   * ID of hardware the license is valid for.
   * Null if the license is available on all hardware.
   */
  private final List<String> hardwareIds_;

  /** Product name. Non-null. */
  private final String product_;

  /** ID of product the license is valid for. Non-null. */
  private final String productId_;

  /** Features of application available under this license. Non-null. */
  private final List<String> features_ = new ArrayList<>();

  /** Date issued. Non-null. */
  private final Date issuedDate_;

  /** Last date the issue is valid. Or null if it never expires. */
  private final Date expireDate_;

  /**
   * Create a new license.
   *
   * @param licensee    Licensee, i.e. license holder. Non-null.
   * @param issuer      License issuer. Non-null.
   * @param hardwareIds Hardware IDs the license is valid on. Null if all.
   * @param product     Product the license is for. Non-null.
   * @param productId   ID of product instance the license is for. Non-null.
   * @param features    List of enabled features. Non-null.
   * @param issuedDate  Date of issue. Non-null.
   * @param expireDate  Date of expiration. Null if license never expires.
   * @throws IllegalArgumentException  If licensee, issuer, product, productId,
   *                    features or issuedDate is null.
   */
  License(String licensee,
          String issuer,
          List<String> hardwareIds,
          String product,
          String productId,
          List<String> features,
          Date issuedDate,
          Date expireDate)
  {
    if (licensee == null)
      throw new IllegalArgumentException("licensee cannot be null");

    if (issuer == null)
      throw new IllegalArgumentException("issuer cannot be null");

    if (product == null)
      throw new IllegalArgumentException("product cannot be null");

    if (productId == null)
      throw new IllegalArgumentException("productId cannot be null");

    if (features == null)
      throw new IllegalArgumentException("features cannot be null");

    if (issuedDate == null)
      throw new IllegalArgumentException("issuedDate cannot be null");

    issuer_ = issuer;
    licensee_ = licensee;
    hardwareIds_ = hardwareIds == null || hardwareIds.isEmpty() ? null : new ArrayList<>(hardwareIds);
    product_ = product;
    productId_ = productId;
    features_.addAll(features);
    issuedDate_ = new Date(issuedDate.getTime());
    expireDate_ = expireDate != null ? new Date(expireDate.getTime()) : null;
  }

  /**
   * Return licensee of this license.
   *
   * @return  Licensee of this license. Never null.
   */
  public String getLicensee()
  {
    return licensee_;
  }

  /**
   * Return issuer of this license.
   *
   * @return  Issuer of this license. Never null.
   */
  public String getIssuer()
  {
    return issuer_;
  }

  /**
   * Check if the license is valid for the present hardware.
   *
   * @return  True if the license is valid for the present hardware,
   *          false otherwise.
   */
  public boolean isValidForThisHardware()
  {
    if (hardwareIds_ == null)
      return true;

    String hardwareId = HardwareId.get();
    return hardwareIds_.contains(hardwareId);
  }

  /**
   * Check if the license is valid for all hardware.
   *
   * @return  True if the license is valid for all hardware,
   *          false if it is for specific hardware only.
   */
  public boolean isValidForAllHardware()
  {
    return hardwareIds_ == null;
  }

  /**
   * Return hardware IDs this license is valid on.
   *
   * @return  Hardware ID this license is valid on,
   *          or null if it is valid across all hardware.
   */
  public List<String> getHardwareIds()
  {
    return hardwareIds_ == null ? null : Collections.unmodifiableList(hardwareIds_);
  }

  /**
   * Return valid hardware as a string.
   *
   * @return  Valid hardware as a string.
   */
  public String getHardware()
  {
    if (hardwareIds_ == null)
      return "any hardware";

    StringBuilder s = new StringBuilder();
    for (int i = 0; i < hardwareIds_.size(); i++) {
      if (i > 0)
        s.append(",");
      s.append(hardwareIds_.get(i));
    }

    return s.toString();
  }

  /**
   * Return name of product this license is for.
   *
   * @return  Name of product of this license. Never null.
   */
  public String getProduct()
  {
    return product_;
  }

  /**
   * Return ID of the specific product instance this license is valid for.
   *
   * @return  Product ID this license is valid for. Never null.
   */
  public String getProductId()
  {
    return productId_;
  }

  /**
   * Return the application features that are available under the
   * present license.
   *
   * @return  Available application features. Never null.
   */
  public List<String> getFeatures()
  {
    return Collections.unmodifiableList(features_);
  }

  /**
   * Check if the specified application features is available under
   * this license.
   *
   * @param feature  Feature to check. Non-null.
   * @return         True is the feature is available, false otherwise.
   * @throws IllegalArgumentException  If feature is null.
   */
  public boolean hasFeature(String feature)
  {
    if (feature == null)
      throw new IllegalArgumentException("feature cannot be null");

    return features_.contains(feature) && !isExpired();
  }

  /**
   * Return issue date of this license.
   *
   * @return  Issue date of this license. Never null.
   */
  public Date getIssuedDate()
  {
    return new Date(issuedDate_.getTime());
  }

  /**
   * Return expiration date of this license.
   *
   * @return  Expiration date of this license. Null if it never expires.
   */
  public Date getExpireDate()
  {
    return expireDate_ != null ? new Date(expireDate_.getTime()) : null;
  }

  /**
   * Return the number of days until the license expires.
   * If the license has already expired, 0 is returned.
   * If there is no limit on the license, Integer.MAX_VALUE is returned.
   *
   * @return  Number of days until the license expires. [0,&gt;.
   */
  public int getNDaysLeft()
  {
    if (expireDate_ == null)
      return Integer.MAX_VALUE;

    Date now = new Date();
    long difference = expireDate_.getTime() - now.getTime();

    long nDays = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);

    return nDays < 0L ? 0 : (int) nDays + 1;
  }

  /**
   * Check if the license has expired.
   *
   * @return  True if the license has expired, false otherwise.
   */
  public boolean isExpired()
  {
    return getNDaysLeft() == 0;
  }

  /**
   * Check if this license is valid for the current session,
   * i.e. that it is valid for the present hardware and that it has not expired.
   *
   * @return  True if the license is valid for the current session, false otherwise.
   */
  public boolean isValid()
  {
    return isValidForThisHardware() && !isExpired();
  }


  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();

    s.append("Issuer = " + issuer_ + "\n");
    s.append("Licensee = " + licensee_ + "\n");
    s.append("Hardware ID = " + getHardware() + "\n");
    s.append("Product = " + product_ + " (" + productId_ + ")\n");
    s.append("Features = ");
    for (int i = 0; i < features_.size(); i++) {
      String feature = features_.get(i);
      if (i > 0)
        s.append(", ");
      s.append(feature);
    }
    s.append("\n");
    s.append("Issued = " + issuedDate_ + "\n");
    s.append("Expires = ");
    s.append(expireDate_ == null ? "never" : expireDate_);

    return s.toString();
  }
}