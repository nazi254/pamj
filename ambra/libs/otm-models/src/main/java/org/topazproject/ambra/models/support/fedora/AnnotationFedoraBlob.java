/* $HeadURL::                                                                            $
 * $Id$
 *
 * Copyright (c) 2007-2008 by Topaz, Inc.
 * http://topazproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.topazproject.ambra.models.support.fedora;

import org.topazproject.ambra.models.AnnotationBlob;
import org.topazproject.ambra.models.ReplyBlob;

import org.topazproject.fedora.otm.DefaultFedoraBlob;

import org.topazproject.otm.ClassMetadata;
import org.topazproject.otm.EntityMode;
import org.topazproject.otm.mapping.java.ClassBinder;

/**
 * A FedoraBlob implementation for Annotations and Replies.
 *
 * @author Pradeep Krishnan
 */
public class AnnotationFedoraBlob extends DefaultFedoraBlob {
  /**
   * Creates a new AnnotationFedoraBlob object.
   *
   * @param cm the class metadata of this blob
   * @param pid the Fedora PID of this blob
   * @param dsId the Datastream id of this blob
   *
   */
  public AnnotationFedoraBlob(ClassMetadata cm, String pid, String dsId) {
    super(cm, pid, dsId);
  }

  /**
   * Gets the contentType for use in the FOXML. Defaults to "text/plain;UTF-8".
   *
   * @return the content type
   */
  public String getContentType() {
    return "text/plain;UTF-8";
  }

  /**
   * Gets the content model to use in the FOXML. Will be either "Annotation" or "Reply"
   * depending on the Blob being created.
   *
   * @return the content model
   *
   * @throws Error if the ClassMetadata is for an entity that we don't handle
   */
  public String getContentModel() {
    ClassBinder<?> b = (ClassBinder<?>) getClassMetadata().getEntityBinder(EntityMode.POJO);
    Class<?>       c = b.getSourceClass();

    if (AnnotationBlob.class.isAssignableFrom(c))
      return "Annotation";

    if (ReplyBlob.class.isAssignableFrom(c))
      return "Reply";

    throw new Error("Expecting " + AnnotationBlob.class + " or " + ReplyBlob.class +
                    " only. Instead found " + c);
  }

  /**
   * Gets the datastream label to use in the FOXML. Will be either "Annotation Body" or
   * "Reply Body" based on the Blob being created.
   *
   * @return the label to use
   *
   * @throws Error if the ClassMetadata is for an entity that we don't handle
   */
  public String getDatastreamLabel() {
    ClassBinder<?> b = (ClassBinder<?>) getClassMetadata().getEntityBinder(EntityMode.POJO);
    Class<?>       c = b.getSourceClass();

    if (AnnotationBlob.class.isAssignableFrom(c))
      return "Annotation Body";

    if (ReplyBlob.class.isAssignableFrom(c))
      return "Reply Body";

    throw new Error("Expecting " + AnnotationBlob.class + " or " + ReplyBlob.class +
                    " only. Instead found " + c);
  }

  public INGEST_OP getFirstIngestOp() {
    return INGEST_OP.AddObj;
  }

  public boolean hasSingleDs() {
    return true;
  }
}