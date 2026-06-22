package ir.vrhouse.icodeslix2

object TagWorkflows {
    fun programAndLock(session: Slix2NfcV, logger: (String) -> Unit) {
        logger("UID(raw Android order): ${HexUtils.toHex(session.uidRaw())}")
        logger("UID(display order): ${session.uidHumanReadable()}")
        logger("شروع نوشتن فقط بلاک‌های 0-4 و 48-63 ...")

        IcodeSlix2Config.TARGET_BLOCKS.forEach { (block, data) ->
            session.writeSingleBlock(block, data)
        }

        logger("خواندن مجدد بلاک‌های نوشته‌شده برای verify ...")
        val mismatches = mutableListOf<String>()
        IcodeSlix2Config.TARGET_BLOCKS.forEach { (block, expected) ->
            val actual = session.readSingleBlock(block)
            if (!actual.contentEquals(expected)) {
                mismatches += "Block ${HexUtils.blockLabel(block)} expected=${HexUtils.toHex(expected)} actual=${HexUtils.toHex(actual)}"
            }
        }
        if (mismatches.isNotEmpty()) {
            throw IllegalStateException("Verify failed:\n${mismatches.joinToString("\n")}")
        }
        logger("✅ Verify بلاک‌ها موفق بود.")

        logger("تغییر Read Password از مقدار پیش‌فرض به CF8B1B78 ...")
        session.presentPassword(IcodeSlix2Config.READ_PASSWORD_ID, IcodeSlix2Config.DEFAULT_READ_KEY)
        session.writePassword(IcodeSlix2Config.READ_PASSWORD_ID, IcodeSlix2Config.TARGET_READ_KEY)

        logger("تغییر Write Password از مقدار پیش‌فرض به F1B32A1D ...")
        session.presentPassword(IcodeSlix2Config.WRITE_PASSWORD_ID, IcodeSlix2Config.DEFAULT_WRITE_KEY)
        session.writePassword(IcodeSlix2Config.WRITE_PASSWORD_ID, IcodeSlix2Config.TARGET_WRITE_KEY)

        logger("ارسال authentication با کلیدهای جدید برای عملیات قفل نهایی ...")
        session.presentPassword(IcodeSlix2Config.READ_PASSWORD_ID, IcodeSlix2Config.TARGET_READ_KEY)
        session.presentPassword(IcodeSlix2Config.WRITE_PASSWORD_ID, IcodeSlix2Config.TARGET_WRITE_KEY)

        logger("اجرای سه دستور نهایی: Write access condition، Password Protect، Write DSFID ...")
        session.writeAccessCondition()
        session.passwordProtect64Bit()
        session.writeDsfid(0x03)

        logger("✅ همه مراحل با موفقیت کامل شد. تگ برنامه‌ریزی و قفل شد.")
    }

    fun recoverPasswordsToDefault(session: Slix2NfcV, logger: (String) -> Unit) {
        logger("UID(raw Android order): ${HexUtils.toHex(session.uidRaw())}")
        logger("UID(display order): ${session.uidHumanReadable()}")
        logger("ریکاوری فقط برای تگ‌هایی است که کلیدها تغییر کرده‌اند ولی فیوزبیت‌ها هنوز ست نشده‌اند.")

        logger("بازگردانی Read Password به 00000000 با احراز هویت کلید جدید ...")
        session.presentPassword(IcodeSlix2Config.READ_PASSWORD_ID, IcodeSlix2Config.TARGET_READ_KEY)
        session.writePassword(IcodeSlix2Config.READ_PASSWORD_ID, IcodeSlix2Config.DEFAULT_READ_KEY)

        logger("بازگردانی Write Password به 00000000 با احراز هویت کلید جدید ...")
        session.presentPassword(IcodeSlix2Config.WRITE_PASSWORD_ID, IcodeSlix2Config.TARGET_WRITE_KEY)
        session.writePassword(IcodeSlix2Config.WRITE_PASSWORD_ID, IcodeSlix2Config.DEFAULT_WRITE_KEY)

        logger("تست احراز هویت با کلیدهای پیش‌فرض پس از ریکاوری ...")
        session.presentPassword(IcodeSlix2Config.READ_PASSWORD_ID, IcodeSlix2Config.DEFAULT_READ_KEY)
        session.presentPassword(IcodeSlix2Config.WRITE_PASSWORD_ID, IcodeSlix2Config.DEFAULT_WRITE_KEY)

        logger("✅ ریکاوری انجام شد. Read/Write Password به مقدار پیش‌فرض 00000000 برگشت.")
    }
}
