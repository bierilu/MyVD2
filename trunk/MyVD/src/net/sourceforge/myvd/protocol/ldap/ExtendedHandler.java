package net.sourceforge.myvd.protocol.ldap;

/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */



import java.util.HashMap;

import net.sourceforge.myvd.chain.DeleteInterceptorChain;
import net.sourceforge.myvd.chain.ExetendedOperationInterceptorChain;
import net.sourceforge.myvd.inserts.Insert;
import net.sourceforge.myvd.router.Router;
import net.sourceforge.myvd.types.DistinguishedName;
import net.sourceforge.myvd.types.ExtendedOperation;
import net.sourceforge.myvd.types.Password;

import org.apache.ldap.common.NotImplementedException;
import org.apache.ldap.common.message.DeleteResponse;
import org.apache.ldap.common.message.DeleteResponseImpl;
import org.apache.ldap.common.message.ExtendedRequest;
import org.apache.ldap.common.message.ExtendedResponse;
import org.apache.ldap.common.message.ExtendedResponseImpl;
import org.apache.ldap.common.message.LdapResultImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.handler.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPExtendedOperation;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.ExtendedRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 161724 $
 */
public class ExtendedHandler implements MessageHandler,LdapInfo
{
	
	 private static final Logger LOG = LoggerFactory.getLogger( ExtendedHandler.class );
		private Insert[] globalChain;
		private Router router;
		
    public void messageReceived( ProtocolSession session, Object request )
    {
        ExtendedRequest req = (ExtendedRequest) request;
        ExtendedResponse resp = new ExtendedResponseImpl( req.getMessageId() );
        resp.setLdapResult( new LdapResultImpl( resp ) );
        
HashMap userSession = null;
        
        
        
        try
        {
        	userSession = (HashMap) session.getAttribute("VLDAP_SESSION");
            DistinguishedName bindDN = (DistinguishedName) session.getAttribute("VLDAP_BINDDN");
            Password pass = (Password) session.getAttribute("VLDAP_BINDPASS");
            
            if (bindDN == null) {
            	bindDN = new DistinguishedName("");
            	pass = new Password();
            }
            
            ExetendedOperationInterceptorChain chain = new ExetendedOperationInterceptorChain(bindDN,pass,0,this.globalChain,userSession,new HashMap(),router);
            
            ExtendedOperation op = new ExtendedOperation(null, new LDAPExtendedOperation(req.getOid(),req.getPayload()));
            chain.nextExtendedOperations(op,new LDAPConstraints());
            
        }
        catch( LDAPException e )
        {
            String msg = "failed to delete entry " + "";

           /* if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
            }*/

            ResultCodeEnum code;

           
           code = ResultCodeEnum.getResultCodeEnum(e.getResultCode());
           
            resp.getLdapResult().setResultCode( code );
            resp.getLdapResult().setErrorMessage( msg );

            if( e.getMatchedDN() != null )
            {
                resp.getLdapResult().setMatchedDn( e.getMatchedDN().toString() );
            }

            session.write( resp );
            return;
        } catch (Throwable t) {
        	
            String msg = "failed to perform extended op "  + t.toString();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( t );
            }

            ResultCodeEnum code;

            
                code = ResultCodeEnum.OPERATIONSERROR;
            

            resp.getLdapResult().setResultCode( code );
            resp.getLdapResult().setErrorMessage( msg );
            

            session.write( resp );
            return;
        
    }

        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
        resp.getLdapResult().setMatchedDn( "" );
        session.write( resp );
    }

	public void setEnv(Insert[] globalChain, Router router) {
		this.globalChain = globalChain;
		this.router = router;
		
	}
}