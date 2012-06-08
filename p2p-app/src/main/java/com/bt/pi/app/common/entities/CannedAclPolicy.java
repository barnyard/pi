/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.entities;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * ACL types http://docs.amazonwebservices.com/AmazonS3/latest/index.html?RESTAccessPolicy.html
 */
public enum CannedAclPolicy {
    AWS_EXEC_READ("aws-exec-read"), /* special acl that allows EC2 to read bundled images from buckets but means they're private to everyone else */
    PRIVATE("private"), /* Owner gets FULL_CONTROL. No one else has access rights (default). */
    PUBLIC_READ("public-read"), /*
                                 * Owner gets FULL_CONTROL and the anonymous principal is granted READ access. If this policy is used on an object, it can be read from a browser with no
                                 * authentication.
                                 */
    PUBLIC_READ_WRITE("public-read-write"), /*
                                             * Owner gets FULL_CONTROL, the anonymous principal is granted READ and WRITE access. This can be a useful policy to apply to a bucket, but is generally not
                                             * recommended.
                                             */
    AUTHENTICATED_READ("authenticated-read"), /* Owner gets FULL_CONTROL, and any principal authenticated as a registered Amazon S3 user is granted READ access. */
    BUCKET_OWNER_READ("bucket-owner-read"), /*
                                             * Object Owner gets FULL_CONTROL, Bucket Owner gets READ. This ACL applies only to objects and is equivalent to private when used with PUT Bucket. You use
                                             * this ACL to let someone other than the bucket owner write content (get full control) in the bucket but still grant the bucket owner read access to the
                                             * objects.
                                             */
    BUCKET_OWNER_FULL_CONTROL("bucket-owner-full-control"); /*
                                                             * Object Owner gets FULL_CONTROL, Bucket Owner gets FULL_CONTROL. This ACL applies only to objects and is equivalent to private when used
                                                             * with PUT Bucket. You use this ACL to let someone other than the bucket owner write content (get full control) in the bucket but still
                                                             * grant the bucket owner full rights over the objects.
                                                             */

    private static final Map<String, CannedAclPolicy> LOOKUP = new HashMap<String, CannedAclPolicy>();
    private String name;

    static {
        for (CannedAclPolicy s : EnumSet.allOf(CannedAclPolicy.class))
            LOOKUP.put(s.getName(), s);
    }

    private CannedAclPolicy(String s) {
        this.name = s;
    }

    public String getName() {
        return name;
    }

    public static CannedAclPolicy get(String s) {
        return LOOKUP.get(s);
    }
}
