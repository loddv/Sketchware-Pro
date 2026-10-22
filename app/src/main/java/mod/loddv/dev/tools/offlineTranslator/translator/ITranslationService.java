package mod.loddv.dev.tools.offlineTranslator.translator;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ITranslationService extends IInterface {

    void translate(String textToTranslate, String fromLanguage, String toLanguage, ITranslationCallback callback) throws RemoteException;

    abstract class Stub extends Binder implements ITranslationService {

        private static final String DESCRIPTOR = "dev.davidv.translator.ITranslationService";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static ITranslationService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof ITranslationService) {
                return (ITranslationService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= IBinder.FIRST_CALL_TRANSACTION && code <= IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_translate: {
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    String _arg2 = data.readString();
                    ITranslationCallback _arg3 = ITranslationCallback.Stub.asInterface(data.readStrongBinder());
                    this.translate(_arg0, _arg1, _arg2, _arg3);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        static final int TRANSACTION_translate = IBinder.FIRST_CALL_TRANSACTION + 0;

        private static class Proxy implements ITranslationService {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public void translate(String textToTranslate, String fromLanguage, String toLanguage,
                                  ITranslationCallback callback) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(textToTranslate);
                    data.writeString(fromLanguage);
                    data.writeString(toLanguage);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_translate, data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }
        }
    }
}
