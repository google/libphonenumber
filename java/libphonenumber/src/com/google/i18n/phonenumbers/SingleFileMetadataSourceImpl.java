package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link MetadataSource} that reads from a resource file
 * during initialization.
 */
public final class SingleFileMetadataSourceImpl implements MetadataSource {

  private static final Logger logger =
      Logger.getLogger(SingleFileMetadataSourceImpl.class.getName());

  private static final String META_DATA_FILE =
      "/com/google/i18n/phonenumbers/data/SingleFilePhoneNumberMetadataProto";

  // A mapping from a region code to the PhoneMetadata for that region.
  // Note: Synchronization, though only needed for the Android version of the library, is used in
  // all versions for consistency.
  private final Map<String, PhoneMetadata> regionToMetadataMap =
      Collections.synchronizedMap(new HashMap<String, PhoneMetadata>());

  // A mapping from a country calling code for a non-geographical entity to the PhoneMetadata for
  // that country calling code. Examples of the country calling codes include 800 (International
  // Toll Free Service) and 808 (International Shared Cost Service).
  // Note: Synchronization, though only needed for the Android version of the library, is used in
  // all versions for consistency.
  private final Map<Integer, PhoneMetadata> countryCodeToNonGeographicalMetadataMap =
      Collections.synchronizedMap(new HashMap<Integer, PhoneMetadata>());

  // It is assumed that metadataLoader is not null.
  public SingleFileMetadataSourceImpl(MetadataLoader metadataLoader) {
    InputStream input = metadataLoader.loadMetadata(META_DATA_FILE);
    if (input == null) {
      throw new IllegalStateException(
          "no metadata available for PhoneNumberUtil: " + META_DATA_FILE);
    }
    PhoneMetadataCollection metadataCollection = loadMetadataAndCloseInput(input);
    for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
      String regionCode = metadata.getId();
      int countryCallingCode = metadata.getCountryCode();
      if (PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode)) {
        countryCodeToNonGeographicalMetadataMap.put(countryCallingCode, metadata);
      } else {
        regionToMetadataMap.put(regionCode, metadata);
      }
    }
  }

  @Override
  public PhoneMetadata getMetadataForRegion(String regionCode) {
    return regionToMetadataMap.get(regionCode);
  }

  @Override
  public PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
    return countryCodeToNonGeographicalMetadataMap.get(countryCallingCode);
  }

  /**
   * Loads the metadata protocol buffer from the given stream and closes the stream afterwards. Any
   * exceptions that occur while reading the stream are propagated (though exceptions that occur
   * when the stream is closed will be ignored).
   *
   * @param source  the non-null stream from which metadata is to be read.
   * @return        the loaded metadata protocol buffer.
   */
  private static PhoneMetadataCollection loadMetadataAndCloseInput(InputStream source) {
    PhoneMetadataCollection metadataCollection = new PhoneMetadataCollection();
    try {
      // Read in metadata for each region.
      ObjectInputStream in = new ObjectInputStream(source);
      metadataCollection.readExternal(in);
      return metadataCollection;
    } catch (IOException e) {
      logger.log(Level.WARNING, e.toString());
    } finally {
      try {
        source.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, e.toString());
      }
    }
    return metadataCollection;
  }
}
