/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 
package rice.post.storage;

import java.io.*;
import java.security.*;
import java.util.*;

import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.*;
import rice.p2p.past.*;
import rice.p2p.past.gc.*;

/**
 * This class is used internally by the storage package to
 * store data as a content-hash.
 * 
 * @version $Id: ContentHashData.java 3613 2007-02-15 14:45:14Z jstewart $
 */
class ContentHashData extends StorageServiceData {
  public static final short TYPE = 1;
  
  // serialver, for backwards compatibility
  private static final long serialVersionUID = -8274270442542322772L;
  
  /**
   * Builds a ContentHashData from a byte array and a location
   *
   * @param data The data to store
   * @param credentials Credentials of the data
   */
  public ContentHashData(Id location, byte[] data) {
    super(location, data);
  }

  /**
   * Checks to see if the provided object is equal to this one
   * 
   * @param o The object to check
   * @return Whether or not it's equal
   */
  public boolean equals(Object o) {
    if (! (o instanceof ContentHashData))
      return false;

    return Arrays.equals(data, ((ContentHashData) o).getData());
  }

  /**
   * Returns a string represetation of this object
   *
   * @return A string
   */
  public String toString() {
    return "ContentHashData[" + data.length + "]";
  }

  /**
   * Checks if a insert operation should be allowed.  Invoked when a
   * Past node receives an insert request and it is a replica root for
   * the id; invoked on the object to be inserted.  This method
   * determines the effect of an insert operation on an object that
   * already exists: it computes the new value of the stored object,
   * as a function of the new and the existing object.
   *
   * @param id the key identifying the object
   * @param newObj the new object to be stored
   * @param existingObj the existing object stored on this node (null if no object associated with id is stored on this node)
   * @return null, if the operation is not allowed; else, the new
   * object to be stored on the local node.
   */
  public PastContent checkInsert(rice.p2p.commonapi.Id id, PastContent existingContent) throws PastException {
    if (existingContent == null) {
      return this;
    } else {
      return existingContent;
    }
  }

  /**
   * States if this content object is mutable. Mutable objects are not subject to dynamic caching in Past.
   *
   * @return true if this object is mutable, else false
   */
  public boolean isMutable() {
    return false;
  }

  public short getType() {
    return TYPE;
  }
  
  public ContentHashData(InputBuffer buf, Endpoint endpoint) throws IOException {
    super(buf, endpoint); 
  }
}
