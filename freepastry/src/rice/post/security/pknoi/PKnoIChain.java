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
package rice.post.security.pknoi;

import java.io.*;
import java.security.*;

import rice.post.*;
import rice.post.security.*;

/**
 * This class is the notion of a chain of "vouches" from one user to another in the
 * PKnoI POST system.  The chain can contain other metadata, such as the "validity"
 * of this chain (based on some metric) or similar information.
 *
 * @version $Id: PKnoIChain.java 3613 2007-02-15 14:45:14Z jstewart $
 * @author amislove
 */
public class PKnoIChain implements Serializable {

  /**
   * Builds a PKnoIChain from the to and from certificates, and the chain of
   * signatures which verify each other
   *
   * @param from The origin user
   * @param to The destination user
   * @param sigs The array of signatures
   */
  protected PKnoIChain(PKnoIPostCertificate from, PKnoIPostCertificate to, PKnoISignature[] sigs) {
  }

  /**
   * Returns the origin user for this cahin
   *
   * @return The origin of the chain
   */
  public PKnoIPostCertificate getFrom() {
    return null;
  }

  /**
   * Returns the destination user for this cahin
   *
   * @return The destination of the chain
   */
  public PKnoIPostCertificate getTo() {
    return null;
  }

  /**
   * Returns the signatures for this cahin
   *
   * @return The signatures for the chain
   */
  public PKnoISignature[] getSignatures() {
    return null;
  }
}
