package nz.geek.ss23.balanceit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] intentFiltersArray;
    private AlertDialog mDialog;

    private TextView balanceView;
    private TextView cardPrompt;

    private final byte[] key = new byte[] { (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0xff, (byte)0xff, (byte)0xff };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(nz.geek.ss23.balanceit.R.layout.activity_main);

        balanceView = findViewById(nz.geek.ss23.balanceit.R.id.balanceView);
        cardPrompt = findViewById(nz.geek.ss23.balanceit.R.id.cardPrompt);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(nz.geek.ss23.balanceit.R.string.error, nz.geek.ss23.balanceit.R.string.no_nfc);
            finish();
            return;
        }

        resolveIntent(getIntent());

        // Set up a filter to only receive the correct pending intent
        intentFiltersArray = new IntentFilter[] {new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED), };
        mPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mAdapter.isEnabled()) {
            showWirelessSettingsDialog();
        }
        //mAdapter.enableForegroundDispatch(this, mPendingIntent, intentFiltersArray, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mAdapter.disableForegroundDispatch(this);
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(nz.geek.ss23.balanceit.R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        try {
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
                Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                MifareClassic tag = MifareClassic.get(tagFromIntent);

                //Toast.makeText(getApplicationContext(), "Card detected!", Toast.LENGTH_SHORT).show();
                //Log.d("BIT", "Card detected");

                tag.connect();

                // Authenticate to the card in the sector we'll be reading from
                if (!tag.authenticateSectorWithKeyA(2, key)) {
                    //Log.d("BIT", "Authentication failed. Not Busit?");
                    Toast.makeText(getApplicationContext(), "Invalid card detected.", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    Toast.makeText(getApplicationContext(), "Reading card", Toast.LENGTH_SHORT).show();
                }

                // Hide our "Put your card up" prompt if we get here
                cardPrompt.setVisibility(View.INVISIBLE);

                // Read out the bytes we require (sector 2, block 1)
                byte[] blockData = tag.readBlock(tag.sectorToBlock(2) + 1);

                // Reconstruct the balance from our block
                int balance =
                        (blockData[9]  << 0)&0x000000ff |
                        (blockData[10] << 8)&0x0000ff00;

                Log.i("BIT", String.format("9: 0x%x - 10: 0x%x", blockData[9], blockData[10]));

                int cents = balance % 100;
                int dollars = balance / 100;

                balanceView.setText(String.format(Locale.US, "$%d.%02d", dollars, cents));
                balanceView.setVisibility(View.VISIBLE);

                //Toast.makeText(getApplicationContext(), String.format("Balance: $%d.%02d", dollars, cents), Toast.LENGTH_SHORT).show();

                /*

                int sectorCount = tag.getSectorCount(); // Get the number of sectors for this tag
                int tagSize = tag.getSize(); // Get the tag size
                Log.d("SECTOR", Integer.toString(sectorCount));
                Log.d("TAGSIZE", Integer.toString(tagSize));

                if (!tag.authenticateSectorWithKeyA(0, this.key)) {
                    Log.d("AUTH", "Authentication failed. Not Busit?");
                    Toast.makeText(getApplicationContext(), "Invalid card detected.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(getApplicationContext(), "Authenticated!", Toast.LENGTH_SHORT).show();

                // Loop over every sector and authenticate and get the data
                Log.d("DEBUG", String.format("Reading %d sectors", sectorCount));
                for (int i = 0; i < sectorCount; i++) {
                    if (!tag.authenticateSectorWithKeyA(i, this.key)) {
                        Log.d("AUTH", "Authentication failed. Not Mifare?");
                        Toast.makeText(getApplicationContext(), "Invalid card detected.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Loop over every block
                    int blockCount = tag.getBlockCountInSector(i);
                    Log.d("DEBUG", String.format("Reading %d blocks", blockCount));
                    for (int j = 0; j < blockCount; j++) {
                        try {
                            Log.d("DEBUG", String.format("Reading block: %d", tag.sectorToBlock(i) + j));
                            byte[] blockData = tag.readBlock(tag.sectorToBlock(i) + j);
                            for (int k = 0; k < blockData.length; k++) {
                                Log.i("DATA", String.format("%d %d %d: 0x%x", i, j, k, blockData[k]));
                            }
                        } catch (Exception e) {
                            Log.i("ERROR", "Temporary error reading card");
                            // re-authenticate
                            tag.authenticateSectorWithKeyA(i, this.key);
                        }
                    }
                }
                // Retrieve the balance data
                */

            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Unspecified error", Toast.LENGTH_SHORT).show();
            Log.i("BIT", e.toString());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        //Log.i("BIT", "Intent received");
        setIntent(intent);
        resolveIntent(intent);
    }
}
