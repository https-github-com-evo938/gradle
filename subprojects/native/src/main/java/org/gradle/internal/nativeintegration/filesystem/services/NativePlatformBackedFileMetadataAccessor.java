/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.nativeintegration.filesystem.services;

import net.rubygrapefruit.platform.NativeException;
import net.rubygrapefruit.platform.file.FileInfo;
import net.rubygrapefruit.platform.file.Files;
import org.gradle.internal.file.FileMetadataSnapshot;
import org.gradle.internal.file.impl.DefaultFileMetadataSnapshot;
import org.gradle.internal.nativeintegration.filesystem.FileMetadataAccessor;

import java.io.File;

public class NativePlatformBackedFileMetadataAccessor implements FileMetadataAccessor {
    private final Files files;

    public NativePlatformBackedFileMetadataAccessor(Files files) {
        this.files = files;
    }

    @Override
    public FileMetadataSnapshot stat(File f) {
        try {
            FileInfo stat = files.stat(f, true);
            switch (stat.getType()) {
                case File:
                    return DefaultFileMetadataSnapshot.file(stat.getLastModifiedTime(), stat.getSize());
                case Directory:
                    return DefaultFileMetadataSnapshot.directory();
                case Missing:
                case Other:
                    return DefaultFileMetadataSnapshot.missing();
                default:
                    throw new IllegalArgumentException("Unrecognised file type: " + stat.getType());
            }
        } catch (NativeException e) {
            return DefaultFileMetadataSnapshot.missing();
        }
    }
}
