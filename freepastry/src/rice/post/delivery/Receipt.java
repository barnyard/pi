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

package rice.post.delivery;

import java.io.*;

import rice.*;
import rice.post.*;
import rice.post.messaging.*;
import rice.p2p.past.*;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.*;


/**
 * The receipt stored in Past
 *
 * @version $Id: Receipt.java 3613 2007-02-15 14:45:14Z jstewart $
 * @author Alan Mislove
 * @author Ansley Post
 * @author Peter Druschel
 */
public class Receipt extends Delivery {
  public static final short TYPE = 171;

  private static final long serialVersionUID = -2762703066657973942L;

  /**
   * The signature
   */
  protected byte[] signature;
  
  /**
  * Constructor which takes the wrapped message
   *
   * @param message The message to deliver
   */
  protected Receipt(SignedPostMessage message, Id id, byte[] signature) {
    super(message, id);
    this.signature = signature;
  }
  
  /**
   * Returns the internal signature
   *
   * @return The wrapped signature
   */
  public byte[] getSignature() {
    return signature;
  }
  
  public Receipt(InputBuffer buf, Endpoint endpoint) throws IOException {
    super(buf, endpoint); 
    
    signature = new byte[buf.readInt()];
    buf.read(signature);
  }
  
  public void serialize(OutputBuffer buf) throws IOException {
    super.serialize(buf); 
    
    buf.writeInt(signature.length);
    buf.write(signature, 0, signature.length);
    
  }
  
  public short getType() {
    return TYPE;
  }
  
  
}





