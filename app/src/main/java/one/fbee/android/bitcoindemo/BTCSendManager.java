package one.fbee.android.bitcoindemo;

import android.text.TextUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class BTCSendManager {

    private static final String privateKey = "a18054e0753d7e3e97217b7ebfc0818731b20b867df23cc0320109f9e402f2da";
    private static final String seedCode = "feel derive snow favorite slam word rebel kiwi arch tank  wink task";

    public static Wallet importWalletFromMnemonicCode(String mnemonicCode) throws UnreadableWalletException, IOException {
//        NetworkParameters params = TestNet3Params.get();
        NetworkParameters params = getNetworkParameter();
        DeterministicSeed seed = new DeterministicSeed(mnemonicCode, null, "", System.currentTimeMillis());
        Wallet restoredWallet = Wallet.fromSeed(params, seed);
        File file = getWalletDirectory();
        System.out.println("importWalletFromMnemonicCode  address " + restoredWallet.currentReceiveAddress());
        System.out.println("importWalletFromMnemonicCode  address " + restoredWallet.freshReceiveAddress());
        File directory = file.getAbsoluteFile().getParentFile();
        File temp = new File(directory, "seedWallet");
        restoredWallet.saveToFile(temp);
        return restoredWallet;
    }

    public static void main(String[] args) throws Exception {
        Wallet wallet = importWalletFromMnemonicCode(seedCode);
//        importFromProvateKey(privateKey);
//        WalletAppKit walletAppKit = new WalletAppKit(getNetworkParameter(), getWalletDirectory(), "wallet");
        WalletAppKit walletAppKit = new WalletAppKit(getNetworkParameter(), getWalletDirectory(), "seedWallet");
        System.out.println("download start ");
        walletAppKit.startAsync();
        walletAppKit.setDownloadListener(new DownloadProgressTracker());
        walletAppKit.awaitRunning();
        System.out.println("download finish ");
        System.out.println(walletAppKit.wallet().getBalance().toFriendlyString());
    }

    private static void syncBlock() {

    }


    public static Wallet importFromProvateKey(String privateKey) throws IOException {
        BriefLogFormatter.init();
        NetworkParameters params = getNetworkParameter();
        ECKey ecKey = ECKey.fromPrivate(new BigInteger(privateKey, 16));

        Wallet restoredWallet = Wallet.fromKeys(params, Arrays.asList(ecKey));
        System.out.println("importFromProvateKey importFromProvateKey address " + restoredWallet.currentReceiveAddress());
        System.out.println("importFromProvateKey wallet address " + restoredWallet.freshReceiveAddress());
        System.out.println("importFromProvateKey wallet value " + restoredWallet.getBalance().toFriendlyString());
        File file = getWalletDirectory();
        File directory = file.getAbsoluteFile().getParentFile();
        File temp = new File(directory, "wallet");
        restoredWallet.saveToFile(temp);
        return restoredWallet;
    }

    private static File getWalletDirectory() {
        return new File(".");
    }


    public static String send(WalletAppKit walletAppKit, String recipientAddress, String amount) {
        NetworkParameters params = getNetworkParameter();
        String err = "";
        if (TextUtils.isEmpty(recipientAddress) || recipientAddress.equals("Scan recipient QR")) {
            err = "Select recipient";
            return err;
        }
        if (TextUtils.isEmpty(amount) | Double.parseDouble(amount) <= 0) {
            err = "Select valid amount";
            return err;

        }
        if (walletAppKit.wallet().getBalance().isLessThan(Coin.parseCoin(amount))) {
            err = "You got not enough coins";
            return err;
        }
        SendRequest request = SendRequest.to(Address.fromBase58(params, recipientAddress), Coin.parseCoin(amount));
        try {
            walletAppKit.wallet().completeTx(request);
            walletAppKit.wallet().commitTx(request.tx);
            walletAppKit.peerGroup().broadcastTransaction(request.tx).broadcast();
            return "";
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //    //发送交易
    public static void send(Wallet wallet, String recipientAddress, String amount) {
        NetworkParameters params = getNetworkParameter();
        Address targetAddress = Address.fromBase58(params, recipientAddress);
        // Do the send of 1 BTC in the background. This could throw InsufficientMoneyException.
        SPVBlockStore blockStore = null;
        try {
            blockStore = new SPVBlockStore(params, getBLockFile());
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }
        BlockChain chain = null;
        try {
            chain = new BlockChain(params, wallet, blockStore);
            PeerGroup peerGroup = new PeerGroup(params, chain);
            try {
                Wallet.SendResult result = wallet.sendCoins(peerGroup, targetAddress, Coin.parseCoin(amount));
                // Save the wallet to disk, optional if using auto saving (see below).
                //wallet.saveToFile(....);
                // Wait for the transaction to propagate across the P2P network, indicating acceptance.
                try {
                    Transaction transaction = result.broadcastComplete.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                return;
            } catch (InsufficientMoneyException e) {
                e.printStackTrace();
            }
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }

    }

    public static File getBLockFile() {
        File file = new File("/tmp/bitcoin-blocks");
        if (!file.exists()) {
            try {
                boolean newFile = file.createNewFile();
                if (newFile) {
                    return file;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private static NetworkParameters getNetworkParameter() {
        return TestNet3Params.get();
//        return MainNetParams.get();
    }
}


