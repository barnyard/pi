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
package rice.post.security;

import java.io.*;
import java.security.*;

import rice.post.*;

/**
 * This class is the abstraction of a certificate in the POST system, regardless
 * of the underlying security model. This class contains an address and a key.
 *
 * @version $Id: PostCertificate.java 3613 2007-02-15 14:45:14Z jstewart $
 * @author amislove
 */
public abstract class PostCertificate implements Serializable {

  private PostEntityAddress address;
  private PublicKey key;

  /**
   * Builds a PostCertificate from a user address and a public key.
   *
   * @param address The address of the user whose certificate this is
   * @param key The key of the user whose certificate this is
   */
  protected PostCertificate(PostEntityAddress address, PublicKey key) {
    this.address = address;
    this.key = key;
  }

  /**
   * Gets the Address attribute of the PostCertificate object
   *
   * @return The Address value
   */
  public PostEntityAddress getAddress() {
    return address;
  }

  /**
   * Gets the Key attribute of the PostCertificate object
   *
   * @return The Key value
   */
  public PublicKey getKey() {
    return key;
  }

  /**
   * Returns whether or not this object is equal
   *
   * @param o The object to compare to
   * @return Whether or not this one is equal.
   */
  public boolean equals(Object o) {
    if (!(o instanceof PostCertificate)) {
      return false;
    }

    PostCertificate cert = (PostCertificate) o;

    return (address.equals(cert.getAddress()) && key.equals(cert.getKey()));
  }
}
