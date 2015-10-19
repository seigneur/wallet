package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.Bip44AccountBacking;
import com.mycelium.wapi.wallet.KeyCipher;

public class Bip44AccountExternalSignature extends Bip44PubOnlyAccount {
   private final ExternalSignatureProvider _sigProvider;

   public Bip44AccountExternalSignature(Bip44AccountContext context, Bip44AccountKeyManager keyManager, NetworkParameters network, Bip44AccountBacking backing, Wapi wapi, ExternalSignatureProvider signatureProvider) {
      super(context, keyManager, network, backing, wapi);
      _sigProvider = signatureProvider;
   }

   @Override
   public Transaction signTransaction(StandardTransactionBuilder.UnsignedTransaction unsigned, KeyCipher cipher)
         throws KeyCipher.InvalidKeyCipher {
      checkNotArchived();
      if (!isValidEncryptionKey(cipher)) {
         throw new KeyCipher.InvalidKeyCipher();
      }

      // Get the signatures from the external signature provider
      Transaction transaction = _sigProvider.sign(unsigned, this);
      return transaction;
   }

   @Override
   public boolean canSpend() {
      return true;
   }

   
   public int getBIP44AccountType() {
	   return _sigProvider.getBIP44AccountType();
   }
}
