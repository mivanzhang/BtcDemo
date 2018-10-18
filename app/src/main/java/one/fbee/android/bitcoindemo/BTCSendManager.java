package one.fbee.android.bitcoindemo;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class BTCSendManager {

    private static final String privateKey = "a18054e0753d7e3e97217b7ebfc0818731b20b867df23cc0320109f9e402f2da";
    private static final String seedCode = "feel derive snow favorite slam word rebel kiwi arch tank  wink task";
    private static final String fileNAme = "release_seedWallet";

    public static Wallet importWalletFromMnemonicCode(String mnemonicCode) throws UnreadableWalletException, IOException {
//        NetworkParameters params = TestNet3Params.get();
        NetworkParameters params = getNetworkParameter();
        DeterministicSeed seed = new DeterministicSeed(mnemonicCode, null, "", Utils.currentTimeSeconds());
        Wallet restoredWallet = Wallet.fromSeed(params, seed);
        File file = getWalletDirectory();
        System.out.println("importWalletFromMnemonicCode  address " + restoredWallet.currentReceiveAddress());
        System.out.println("importWalletFromMnemonicCode  address " + restoredWallet.freshReceiveAddress());
        File directory = file.getAbsoluteFile().getParentFile();
        File temp = new File(directory, fileNAme);
        restoredWallet.saveToFile(temp);
        return restoredWallet;
    }

    public static void main(String[] args) throws Exception {

        WalletAppKit walletAppKit = getWalletKit(seedCode, "wallet");
        System.out.println("download finish " + walletAppKit.wallet().currentReceiveAddress());
        System.out.println(walletAppKit.wallet().getBalance().toFriendlyString());
        send(walletAppKit, walletAppKit.wallet().currentReceiveAddress().toBase58(), "0.0001");
//        Wallet.SendResult result = walletAppKit.wallet().sendCoins(walletAppKit.peerGroup(), walletAppKit.wallet().currentReceiveAddress(), Coin.parseCoin("0.0001"));
//        System.out.println("coins sent. transaction hash: " + result.tx.getHashAsString());
        System.out.println("transfer balance " + walletAppKit.wallet().getBalance().toFriendlyString());

    }

    //通过助记词
    public static WalletAppKit getWalletKit(String seedcode, String walletName) {
        WalletAppKit walletAppKit = new WalletAppKit(getNetworkParameter(), getWalletDirectory(), walletName) {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getImportedKeys().size() < 1) wallet().importKey(new ECKey());
                wallet().allowSpendingUnconfirmedTransactions();
                setupWalletListeners(wallet());
            }
        };

        walletAppKit.setAutoSave(true);
//        if (!TextUtils.isEmpty(seedcode)) {
        try {
            DeterministicSeed seed = new DeterministicSeed(seedcode, null, "", Utils.currentTimeSeconds());
            walletAppKit.restoreWalletFromSeed(seed);
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }
//        }
        System.out.println("startAsync ");
        walletAppKit.startAsync();
        walletAppKit.awaitRunning();
        System.out.println("finish startAsync ");

        return walletAppKit;
    }

    private static NetworkParameters getNetworkParameter() {
        return TestNet3Params.get();
//        return MainNetParams.get();
    }

    public static Wallet importFromProvateKey(String privateKey) throws IOException {
        BriefLogFormatter.init();
        NetworkParameters params = getNetworkParameter();
        ECKey ecKey = ECKey.fromPrivate(new BigInteger(privateKey, 16));
        Wallet restoredWallet = Wallet.fromKeys(params, Arrays.asList(ecKey));
        File file = getWalletDirectory();
        File directory = file.getAbsoluteFile().getParentFile();
        File temp = new File(directory, "wallet");
        restoredWallet.saveToFile(temp);
        return restoredWallet;
    }

    private static File getWalletDirectory() {
        return new File(".");
    }

    public static String simpleSend(WalletAppKit walletAppKit, String recipientAddress, String amount) throws InsufficientMoneyException {
        Wallet.SendResult result = walletAppKit.wallet().sendCoins(walletAppKit.peerGroup(), walletAppKit.wallet().currentReceiveAddress(), Coin.parseCoin(amount));
        System.out.println("coins sent. transaction hash: " + result.tx.getHashAsString());
        return result.tx.getHashAsString();
    }

    public static String send(WalletAppKit walletAppKit, String recipientAddress, String amount) {
        System.out.println("send start recipientAddress " + recipientAddress + " amount   " + amount);
        NetworkParameters params = getNetworkParameter();
        String err = "";
//        if (TextUtils.isEmpty(recipientAddress) || recipientAddress.equals("Scan recipient QR")) {
//            err = "Select recipient";
//            return err;
//        }
//        if (TextUtils.isEmpty(amount) | Double.parseDouble(amount) <= 0) {
//            err = "Select valid amount";
//            return err;
//
//        }
        if (walletAppKit.wallet().getBalance().isLessThan(Coin.parseCoin(amount))) {
            err = "You got not enough coins";
            return err;
        }
        SendRequest request = SendRequest.to(Address.fromBase58(params, recipientAddress), Coin.parseCoin(amount));
        try {
            walletAppKit.wallet().completeTx(request);
            walletAppKit.wallet().commitTx(request.tx);
            walletAppKit.peerGroup().broadcastTransaction(request.tx).broadcast();
            return request.tx.getHashAsString();
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //    //发送交易
    public static String send(Wallet wallet, String recipientAddress, String amount) throws ExecutionException, InterruptedException {
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
                Transaction transaction = result.broadcastComplete.get();
                return transaction.getHashAsString();
            } catch (InsufficientMoneyException e) {
                e.printStackTrace();
            }
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }
        return "";
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


    public static void setupWalletListeners(Wallet wallet) {
        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                String s = wallet.getBalance().toFriendlyString();
                String s1 = "";
                if (tx.getPurpose() == Transaction.Purpose.UNKNOWN) {
                    s1 = newBalance.minus(prevBalance).toFriendlyString();
                }
            }
        });
        wallet.addCoinsSentEventListener(new WalletCoinsSentEventListener() {
            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                String s = wallet.getBalance().toFriendlyString();
                String s1 = "Sent " + prevBalance.minus(newBalance).minus(tx.getFee()).toFriendlyString();
            }
        });
    }
}


